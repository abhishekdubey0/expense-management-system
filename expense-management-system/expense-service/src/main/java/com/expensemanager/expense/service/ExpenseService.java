package com.expensemanager.expense.service;

import com.expensemanager.expense.dto.request.CreateExpenseRequest;
import com.expensemanager.expense.dto.request.UpdateExpenseRequest;
import com.expensemanager.expense.dto.response.ExpenseResponse;
import com.expensemanager.expense.entity.*;
import com.expensemanager.expense.exception.ExpenseException;
import com.expensemanager.expense.exception.ResourceNotFoundException;
import com.expensemanager.expense.kafka.ExpenseEvent;
import com.expensemanager.expense.kafka.ExpenseEventProducer;
import com.expensemanager.expense.repository.CategoryRepository;
import com.expensemanager.expense.repository.ExpenseAuditLogRepository;
import com.expensemanager.expense.repository.ExpenseRepository;
import com.expensemanager.expense.s3.S3StorageService;
import com.expensemanager.expense.util.ExpenseMapper;
import com.expensemanager.expense.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ExpenseService {

    private final ExpenseRepository       expenseRepository;
    private final CategoryRepository      categoryRepository;
    private final ExpenseAuditLogRepository auditLogRepository;
    private final S3StorageService        s3StorageService;
    private final ExpenseEventProducer    eventProducer;
    private final ExpenseMapper           expenseMapper;

    // ─── CREATE ───────────────────────────────────────────────

    @Transactional
    public ExpenseResponse createExpense(CreateExpenseRequest request, SecurityUtils.UserContext ctx) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));

        Expense expense = Expense.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .amount(request.getAmount())
                .currency(request.getCurrency() != null ? request.getCurrency() : "INR")
                .expenseDate(request.getExpenseDate())
                .category(category)
                .status(ExpenseStatus.DRAFT)
                .submitterId(ctx.getUuid())
                .submitterEmail(ctx.getEmail())
                .submitterName(ctx.getFullName())
                .department(request.getDepartment() != null
                        ? request.getDepartment() : ctx.getDepartment())
                .projectCode(request.getProjectCode())
                .notes(request.getNotes())
                .build();

        Expense saved = expenseRepository.save(expense);
        auditAction(saved, "CREATED", null, ExpenseStatus.DRAFT, ctx, null);

        log.info("Expense created: {} by {}", saved.getUuid(), ctx.getEmail());
        return expenseMapper.toResponse(saved);
    }

    // ─── READ ─────────────────────────────────────────────────

    public ExpenseResponse getExpenseByUuid(String uuid, SecurityUtils.UserContext ctx) {
        Expense expense = findExpenseByUuid(uuid);
        assertCanView(expense, ctx);
        return expenseMapper.toResponse(expense);
    }

    public Page<ExpenseResponse> getMyExpenses(String submitterId, ExpenseStatus status,
                                               int page, int size, String sortBy) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).descending());
        Page<Expense> expenses = (status != null)
                ? expenseRepository.findBySubmitterIdAndStatusAndDeletedFalse(submitterId, status, pageable)
                : expenseRepository.findBySubmitterIdAndDeletedFalse(submitterId, pageable);
        return expenses.map(expenseMapper::toResponse);
    }

    public Page<ExpenseResponse> getAllExpenses(ExpenseStatus status, String department,
                                               Long categoryId, LocalDate from, LocalDate to,
                                               int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return expenseRepository.findWithFilters(status, null, department, categoryId, from, to, pageable)
                .map(expenseMapper::toResponse);
    }

    public Page<ExpenseResponse> searchExpenses(String submitterId, String query, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return expenseRepository.searchBySubmitter(submitterId, query, pageable)
                .map(expenseMapper::toResponse);
    }

    public List<ExpenseResponse> getExpenseAuditTrail(String uuid, SecurityUtils.UserContext ctx) {
        Expense expense = findExpenseByUuid(uuid);
        assertCanView(expense, ctx);
        return auditLogRepository.findByExpenseIdOrderByCreatedAtAsc(expense.getId())
                .stream()
                .map(expenseMapper::auditToResponse)
                .toList();
    }

    // ─── UPDATE ───────────────────────────────────────────────

    @Transactional
    public ExpenseResponse updateExpense(String uuid, UpdateExpenseRequest request,
                                         SecurityUtils.UserContext ctx) {
        Expense expense = findExpenseByUuid(uuid);
        assertIsOwner(expense, ctx);
        assertIsDraft(expense, "update");

        if (request.getTitle()       != null) expense.setTitle(request.getTitle());
        if (request.getDescription() != null) expense.setDescription(request.getDescription());
        if (request.getAmount()      != null) expense.setAmount(request.getAmount());
        if (request.getCurrency()    != null) expense.setCurrency(request.getCurrency());
        if (request.getExpenseDate() != null) expense.setExpenseDate(request.getExpenseDate());
        if (request.getNotes()       != null) expense.setNotes(request.getNotes());
        if (request.getProjectCode() != null) expense.setProjectCode(request.getProjectCode());
        if (request.getCategoryId()  != null) {
            Category cat = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));
            expense.setCategory(cat);
        }

        Expense updated = expenseRepository.save(expense);
        auditAction(updated, "UPDATED", ExpenseStatus.DRAFT, ExpenseStatus.DRAFT, ctx, null);
        return expenseMapper.toResponse(updated);
    }

    // ─── SUBMIT ───────────────────────────────────────────────

    @Transactional
    public ExpenseResponse submitExpense(String uuid, SecurityUtils.UserContext ctx) {
        Expense expense = findExpenseByUuid(uuid);
        assertIsOwner(expense, ctx);
        assertIsDraft(expense, "submit");

        ExpenseStatus oldStatus = expense.getStatus();
        expense.setStatus(ExpenseStatus.SUBMITTED);
        expense.setSubmittedAt(LocalDateTime.now());
        Expense saved = expenseRepository.save(expense);

        auditAction(saved, "SUBMITTED", oldStatus, ExpenseStatus.SUBMITTED, ctx, null);

        // Publish Kafka event for notification-service
        eventProducer.publishExpenseSubmitted(ExpenseEvent.builder()
                .eventType("EXPENSE_SUBMITTED")
                .expenseUuid(saved.getUuid())
                .expenseTitle(saved.getTitle())
                .amount(saved.getAmount())
                .currency(saved.getCurrency())
                .status(saved.getStatus().name())
                .submitterId(ctx.getUuid())
                .submitterEmail(ctx.getEmail())
                .submitterName(ctx.getFullName())
                .performedByEmail(ctx.getEmail())
                .performedByName(ctx.getFullName())
                .department(saved.getDepartment())
                .build());

        log.info("Expense {} submitted by {}", uuid, ctx.getEmail());
        return expenseMapper.toResponse(saved);
    }

    // ─── RECEIPT UPLOAD ───────────────────────────────────────

    @Transactional
    public ExpenseResponse uploadReceipt(String uuid, MultipartFile file,
                                          SecurityUtils.UserContext ctx) {
        Expense expense = findExpenseByUuid(uuid);
        assertIsOwner(expense, ctx);

        if (expense.getStatus() != ExpenseStatus.DRAFT &&
            expense.getStatus() != ExpenseStatus.SUBMITTED) {
            throw new ExpenseException("Cannot upload receipt for expense in status: "
                    + expense.getStatus());
        }

        // Delete old receipt if exists
        if (expense.getReceiptUrl() != null) {
            s3StorageService.deleteReceipt(expense.getReceiptUrl());
        }

        String objectKey = s3StorageService.uploadReceipt(file, expense.getUuid());
        expense.setReceiptUrl(objectKey);
        expense.setReceiptName(file.getOriginalFilename());

        Expense saved = expenseRepository.save(expense);
        auditAction(saved, "RECEIPT_UPLOADED", expense.getStatus(), expense.getStatus(), ctx,
                "Receipt: " + file.getOriginalFilename());

        log.info("Receipt uploaded for expense {} by {}", uuid, ctx.getEmail());
        return expenseMapper.toResponse(saved);
    }

    // ─── SOFT DELETE ──────────────────────────────────────────

    @Transactional
    public void deleteExpense(String uuid, SecurityUtils.UserContext ctx) {
        Expense expense = findExpenseByUuid(uuid);
        assertIsOwner(expense, ctx);
        assertIsDraft(expense, "delete");

        expense.setDeleted(true);
        expenseRepository.save(expense);
        auditAction(expense, "DELETED", expense.getStatus(), expense.getStatus(), ctx, null);
        log.info("Expense {} soft-deleted by {}", uuid, ctx.getEmail());
    }

    // ─── APPROVAL CALLBACKS (called by approval-service) ──────

    @Transactional
    public void markApproved(String uuid, String approverEmail, String approverName) {
        Expense expense = findExpenseByUuid(uuid);
        ExpenseStatus old = expense.getStatus();
        expense.setStatus(ExpenseStatus.APPROVED);
        expense.setApprovedAt(LocalDateTime.now());
        expenseRepository.save(expense);

        log.info("Expense {} marked APPROVED by approver {}", uuid, approverEmail);

        eventProducer.publishExpenseStatusChanged(ExpenseEvent.builder()
                .eventType("EXPENSE_APPROVED")
                .expenseUuid(uuid)
                .expenseTitle(expense.getTitle())
                .amount(expense.getAmount())
                .currency(expense.getCurrency())
                .status(ExpenseStatus.APPROVED.name())
                .submitterId(expense.getSubmitterId())
                .submitterEmail(expense.getSubmitterEmail())
                .submitterName(expense.getSubmitterName())
                .performedByEmail(approverEmail)
                .performedByName(approverName)
                .build());
    }

    @Transactional
    public void markRejected(String uuid, String approverEmail, String approverName, String reason) {
        Expense expense = findExpenseByUuid(uuid);
        expense.setStatus(ExpenseStatus.REJECTED);
        expense.setRejectionReason(reason);
        expenseRepository.save(expense);

        log.info("Expense {} marked REJECTED by {}", uuid, approverEmail);

        eventProducer.publishExpenseStatusChanged(ExpenseEvent.builder()
                .eventType("EXPENSE_REJECTED")
                .expenseUuid(uuid)
                .expenseTitle(expense.getTitle())
                .amount(expense.getAmount())
                .currency(expense.getCurrency())
                .status(ExpenseStatus.REJECTED.name())
                .submitterId(expense.getSubmitterId())
                .submitterEmail(expense.getSubmitterEmail())
                .submitterName(expense.getSubmitterName())
                .performedByEmail(approverEmail)
                .performedByName(approverName)
                .rejectionReason(reason)
                .build());
    }

    @Transactional
    public void markReimbursed(String uuid, String financeEmail, String financeName) {
        Expense expense = findExpenseByUuid(uuid);
        if (expense.getStatus() != ExpenseStatus.APPROVED) {
            throw new ExpenseException("Only APPROVED expenses can be marked as reimbursed");
        }
        expense.setStatus(ExpenseStatus.REIMBURSED);
        expenseRepository.save(expense);
        log.info("Expense {} marked REIMBURSED by {}", uuid, financeEmail);
    }

    // ─── Private Helpers ──────────────────────────────────────

    private Expense findExpenseByUuid(String uuid) {
        return expenseRepository.findByUuidAndDeletedFalse(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Expense", "uuid", uuid));
    }

    private void assertIsOwner(Expense expense, SecurityUtils.UserContext ctx) {
        boolean isAdmin = ctx.hasRole("ROLE_FINANCE_ADMIN") || ctx.hasRole("ROLE_SUPER_ADMIN");
        if (!isAdmin && !expense.getSubmitterId().equals(ctx.getUuid())) {
            throw new ExpenseException("You don't have permission to modify this expense");
        }
    }

    private void assertCanView(Expense expense, SecurityUtils.UserContext ctx) {
        boolean isPrivileged = ctx.hasRole("ROLE_MANAGER")
                || ctx.hasRole("ROLE_FINANCE_ADMIN")
                || ctx.hasRole("ROLE_SUPER_ADMIN");
        if (!isPrivileged && !expense.getSubmitterId().equals(ctx.getUuid())) {
            throw new ExpenseException("You don't have permission to view this expense");
        }
    }

    private void assertIsDraft(Expense expense, String action) {
        if (expense.getStatus() != ExpenseStatus.DRAFT) {
            throw new ExpenseException(
                    "Cannot " + action + " expense in status: " + expense.getStatus() +
                    ". Only DRAFT expenses can be " + action + "d.");
        }
    }

    private void auditAction(Expense expense, String action, ExpenseStatus oldStatus,
                              ExpenseStatus newStatus, SecurityUtils.UserContext ctx, String comment) {
        ExpenseAuditLog log = ExpenseAuditLog.builder()
                .expense(expense)
                .action(action)
                .oldStatus(oldStatus != null ? oldStatus.name() : null)
                .newStatus(newStatus != null ? newStatus.name() : null)
                .performedBy(ctx.getUuid())
                .performedByName(ctx.getFullName())
                .comment(comment)
                .build();
        auditLogRepository.save(log);
    }
}
