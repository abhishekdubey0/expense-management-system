package com.expensemanager.approval.controller;

import com.expensemanager.approval.dto.request.ApprovalDecisionRequest;
import com.expensemanager.approval.dto.response.ApiResponse;
import com.expensemanager.approval.dto.response.ApprovalResponse;
import com.expensemanager.approval.service.ApprovalService;
import com.expensemanager.approval.service.ApprovalService.UserContext;
import com.expensemanager.approval.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/approvals")
@RequiredArgsConstructor
@Tag(name = "Approvals", description = "Multi-level expense approval workflow")
@SecurityRequirement(name = "bearerAuth")
public class ApprovalController {

    private final ApprovalService approvalService;

    /**
     * GET /api/v1/approvals/pending
     * Manager sees level-1 pending; Finance sees level-2 pending.
     */
    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('MANAGER','FINANCE_ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Get pending approvals assigned to current user's role")
    public ResponseEntity<ApiResponse<Page<ApprovalResponse>>> getPending(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        UserContext ctx = SecurityUtils.buildContext();
        Page<ApprovalResponse> result = approvalService.getPendingApprovals(ctx, page, size);
        return ResponseEntity.ok(ApiResponse.success("Pending approvals fetched", result));
    }

    /**
     * POST /api/v1/approvals/{expenseUuid}/approve
     */
    @PostMapping("/{expenseUuid}/approve")
    @PreAuthorize("hasAnyRole('MANAGER','FINANCE_ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Approve an expense")
    public ResponseEntity<ApiResponse<ApprovalResponse>> approve(
            @PathVariable String expenseUuid,
            @Valid @RequestBody ApprovalDecisionRequest decision) {

        UserContext ctx = SecurityUtils.buildContext();
        ApprovalResponse response = approvalService.approve(expenseUuid, decision, ctx);
        return ResponseEntity.ok(ApiResponse.success("Expense approved successfully", response));
    }

    /**
     * POST /api/v1/approvals/{expenseUuid}/reject
     */
    @PostMapping("/{expenseUuid}/reject")
    @PreAuthorize("hasAnyRole('MANAGER','FINANCE_ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Reject an expense (comment/reason required)")
    public ResponseEntity<ApiResponse<ApprovalResponse>> reject(
            @PathVariable String expenseUuid,
            @Valid @RequestBody ApprovalDecisionRequest decision) {

        UserContext ctx = SecurityUtils.buildContext();
        ApprovalResponse response = approvalService.reject(expenseUuid, decision, ctx);
        return ResponseEntity.ok(ApiResponse.success("Expense rejected", response));
    }

    /**
     * GET /api/v1/approvals/my-submissions
     * Employees can track their own submission approval status.
     */
    @GetMapping("/my-submissions")
    @Operation(summary = "Track approval status of my submitted expenses")
    public ResponseEntity<ApiResponse<Page<ApprovalResponse>>> mySubmissions(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        UserContext ctx = SecurityUtils.buildContext();
        Page<ApprovalResponse> result = approvalService.getMySubmissions(ctx.uuid(), page, size);
        return ResponseEntity.ok(ApiResponse.success("Submissions fetched", result));
    }

    /**
     * GET /api/v1/approvals/expense/{expenseUuid}
     * Fetch the full approval chain for a specific expense.
     */
    @GetMapping("/expense/{expenseUuid}")
    @Operation(summary = "Get full approval history for an expense")
    public ResponseEntity<ApiResponse<ApprovalResponse>> getByExpense(
            @PathVariable String expenseUuid) {

        ApprovalResponse response = approvalService.getApprovalByExpenseUuid(expenseUuid);
        return ResponseEntity.ok(ApiResponse.success("Approval details fetched", response));
    }
}
