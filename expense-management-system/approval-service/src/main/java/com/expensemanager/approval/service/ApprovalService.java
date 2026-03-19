package com.expensemanager.approval.service;

import com.expensemanager.approval.dto.request.ApprovalDecisionRequest;
import com.expensemanager.approval.dto.response.ApprovalActionResponse;
import com.expensemanager.approval.dto.response.ApprovalResponse;
import com.expensemanager.approval.entity.*;
import com.expensemanager.approval.exception.ApprovalException;
import com.expensemanager.approval.exception.ResourceNotFoundException;
import com.expensemanager.approval.kafka.ExpenseEvent;
import com.expensemanager.approval.repository.ApprovalActionRepository;
import com.expensemanager.approval.repository.ApprovalRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ApprovalService {

    private final ApprovalRequestRepository requestRepo;
    private final ApprovalActionRepository  actionRepo;
    private final ApprovalPolicyEngine      policyEngine;
    private final ExpenseServiceClient      expenseClient;

    // ─── CREATE approval request (triggered by Kafka event) ───

    @Transactional
    public void createApprovalRequest(ExpenseEvent event) {
        // Idempotent: skip if already exists
        if (requestRepo.existsByExpenseUuidAndStatus(event.getExpenseUuid(), ApprovalStatus.PENDING)) {
            log.warn("Approval request already exists for expense {}", event.getExpenseUuid());
            return;
        }

        int requiredLevels = policyEngine.getRequiredLevels(event.getAmount());

        // Auto-approve small amounts
        if (requiredLevels == 0) {
            log.info("Auto-approving expense {} (amount: {} below threshold)",
                    event.getExpenseUuid(), event.getAmount());
            expenseClient.markExpenseApproved(event.getExpenseUuid(),
                    "system@expensemanager.com", "Auto-Approval System");
            return;
        }

        ApprovalRequest approvalRequest = ApprovalRequest.builder()
                .expenseUuid(event.getExpenseUuid())
                .expenseTitle(event.getExpenseTitle())
                .amount(event.getAmount())
                .currency(event.getCurrency())
                .submitterId(event.getSubmitterId())
                .submitterEmail(event.getSubmitterEmail())
                .submitterName(event.getSubmitterName())
                .department(event.getDepartment())
                .currentLevel(1)
                .totalLevels(requiredLevels)
                .status(ApprovalStatus.PENDING)
                .build();

        requestRepo.save(approvalRequest);
        log.info("Approval request created for expense {} with {} levels",
                event.getExpenseUuid(), requiredLevels);
    }

    // ─── APPROVE ──────────────────────────────────────────────

    @Transactional
    public ApprovalResponse approve(String expenseUuid,
                                     ApprovalDecisionRequest decision,
                                     UserContext ctx) {
        ApprovalRequest request = findPendingByExpenseUuid(expenseUuid);
        validateApproverRole(request, ctx);

        // Record the approval action
        ApprovalAction action = ApprovalAction.builder()
                .approvalRequest(request)
                .level(request.getCurrentLevel())
                .action(ApprovalAction.ActionType.APPROVED)
                .approverId(ctx.getUuid())
                .approverEmail(ctx.getEmail())
                .approverName(ctx.getFullName())
                .approverRole(ctx.getRole())
                .comment(decision.getComment())
                .build();
        actionRepo.save(action);

        // Advance level or complete
        if (request.getCurrentLevel() < request.getTotalLevels()) {
            // Move to next approval level
            request.setCurrentLevel(request.getCurrentLevel() + 1);
            requestRepo.save(request);
            log.info("Expense {} advanced to level {}", expenseUuid, request.getCurrentLevel());
        } else {
            // All levels approved — finalize
            request.setStatus(ApprovalStatus.APPROVED);
            request.setCompletedAt(LocalDateTime.now());
            requestRepo.save(request);

            expenseClient.markExpenseApproved(expenseUuid, ctx.getEmail(), ctx.getFullName());
            log.info("Expense {} fully approved by {}", expenseUuid, ctx.getEmail());
        }

        return toResponse(request);
    }

    // ─── REJECT ───────────────────────────────────────────────

    @Transactional
    public ApprovalResponse reject(String expenseUuid,
                                    ApprovalDecisionRequest decision,
                                    UserContext ctx) {
        if (decision.getComment() == null || decision.getComment().isBlank()) {
            throw new ApprovalException("Rejection reason is required");
        }

        ApprovalRequest request = findPendingByExpenseUuid(expenseUuid);
        validateApproverRole(request, ctx);

        ApprovalAction action = ApprovalAction.builder()
                .approvalRequest(request)
                .level(request.getCurrentLevel())
                .action(ApprovalAction.ActionType.REJECTED)
                .approverId(ctx.getUuid())
                .approverEmail(ctx.getEmail())
                .approverName(ctx.getFullName())
                .approverRole(ctx.getRole())
                .comment(decision.getComment())
                .build();
        actionRepo.save(action);

        request.setStatus(ApprovalStatus.REJECTED);
        request.setCompletedAt(LocalDateTime.now());
        requestRepo.save(request);

        expenseClient.markExpenseRejected(expenseUuid, ctx.getEmail(),
                ctx.getFullName(), decision.getComment());

        log.info("Expense {} rejected by {} — reason: {}",
                expenseUuid, ctx.getEmail(), decision.getComment());
        return toResponse(request);
    }

    // ─── QUERIES ──────────────────────────────────────────────

    public Page<ApprovalResponse> getPendingApprovals(UserContext ctx, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").ascending());

        Page<ApprovalRequest> requests;
        if (ctx.getRole().contains("FINANCE_ADMIN") || ctx.getRole().contains("SUPER_ADMIN")) {
            // Finance sees all pending level-2 requests
            requests = requestRepo.findPendingByLevelAndDepartment(2, null, pageable);
        } else {
            // Manager sees pending level-1 for their department
            requests = requestRepo.findPendingByLevelAndDepartment(1, ctx.getDepartment(), pageable);
        }
        return requests.map(this::toResponse);
    }

    public Page<ApprovalResponse> getMySubmissions(String submitterId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return requestRepo.findBySubmitterIdOrderByCreatedAtDesc(submitterId, pageable)
                .map(this::toResponse);
    }

    public ApprovalResponse getApprovalByExpenseUuid(String expenseUuid) {
        return requestRepo.findByExpenseUuid(expenseUuid)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No approval request found for expense: " + expenseUuid));
    }

    // ─── Private helpers ──────────────────────────────────────

    private ApprovalRequest findPendingByExpenseUuid(String expenseUuid) {
        ApprovalRequest request = requestRepo.findByExpenseUuid(expenseUuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No approval request found for expense: " + expenseUuid));
        if (request.getStatus() != ApprovalStatus.PENDING) {
            throw new ApprovalException("This expense has already been "
                    + request.getStatus().name().toLowerCase());
        }
        return request;
    }

    private void validateApproverRole(ApprovalRequest request, UserContext ctx) {
        int level = request.getCurrentLevel();
        boolean isManager  = ctx.getRole().contains("MANAGER");
        boolean isFinance  = ctx.getRole().contains("FINANCE_ADMIN") || ctx.getRole().contains("SUPER_ADMIN");

        if (level == 1 && !isManager && !isFinance) {
            throw new ApprovalException("Only Managers or Finance Admins can approve at level 1");
        }
        if (level == 2 && !isFinance) {
            throw new ApprovalException("Only Finance Admins can approve at level 2");
        }
        // Prevent self-approval
        if (request.getSubmitterId().equals(ctx.getUuid())) {
            throw new ApprovalException("You cannot approve your own expense");
        }
    }

    private ApprovalResponse toResponse(ApprovalRequest req) {
        List<ApprovalActionResponse> actionResponses = actionRepo
                .findByApprovalRequestIdOrderByActedAtAsc(req.getId())
                .stream()
                .map(a -> ApprovalActionResponse.builder()
                        .level(a.getLevel())
                        .action(a.getAction().name())
                        .approverName(a.getApproverName())
                        .approverEmail(a.getApproverEmail())
                        .approverRole(a.getApproverRole())
                        .comment(a.getComment())
                        .actedAt(a.getActedAt())
                        .build())
                .toList();

        return ApprovalResponse.builder()
                .id(req.getId())
                .uuid(req.getUuid())
                .expenseUuid(req.getExpenseUuid())
                .expenseTitle(req.getExpenseTitle())
                .amount(req.getAmount())
                .currency(req.getCurrency())
                .submitterName(req.getSubmitterName())
                .submitterEmail(req.getSubmitterEmail())
                .department(req.getDepartment())
                .status(req.getStatus().name())
                .currentLevel(req.getCurrentLevel())
                .totalLevels(req.getTotalLevels())
                .actions(actionResponses)
                .createdAt(req.getCreatedAt())
                .completedAt(req.getCompletedAt())
                .build();
    }

    // ─── Inner context class ──────────────────────────────────

    public record UserContext(String uuid, String email, String fullName,
                               String role, String department) {
        public boolean hasRole(String r) { return role != null && role.contains(r); }
    }
}
