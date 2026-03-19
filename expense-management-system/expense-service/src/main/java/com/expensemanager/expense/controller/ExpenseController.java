package com.expensemanager.expense.controller;

import com.expensemanager.expense.dto.request.CreateExpenseRequest;
import com.expensemanager.expense.dto.request.UpdateExpenseRequest;
import com.expensemanager.expense.dto.response.ApiResponse;
import com.expensemanager.expense.dto.response.ExpenseResponse;
import com.expensemanager.expense.entity.ExpenseStatus;
import com.expensemanager.expense.service.ExpenseService;
import com.expensemanager.expense.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/expenses")
@RequiredArgsConstructor
@Tag(name = "Expenses", description = "Expense management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class ExpenseController {

    private final ExpenseService expenseService;

    // ─── CREATE ───────────────────────────────────────────────

    @PostMapping
    @Operation(summary = "Create a new expense (saved as DRAFT)")
    public ResponseEntity<ApiResponse<ExpenseResponse>> createExpense(
            @Valid @RequestBody CreateExpenseRequest request) {

        SecurityUtils.UserContext ctx = SecurityUtils.getCurrentUser();
        ExpenseResponse response = expenseService.createExpense(request, ctx);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Expense created successfully", response));
    }

    // ─── READ ─────────────────────────────────────────────────

    @GetMapping("/{uuid}")
    @Operation(summary = "Get expense by UUID")
    public ResponseEntity<ApiResponse<ExpenseResponse>> getExpense(@PathVariable String uuid) {
        SecurityUtils.UserContext ctx = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.success("Expense fetched",
                expenseService.getExpenseByUuid(uuid, ctx)));
    }

    @GetMapping("/my")
    @Operation(summary = "Get my expenses (paginated, filterable by status)")
    public ResponseEntity<ApiResponse<Page<ExpenseResponse>>> getMyExpenses(
            @RequestParam(required = false) ExpenseStatus status,
            @RequestParam(defaultValue = "0")   int page,
            @RequestParam(defaultValue = "10")  int size,
            @RequestParam(defaultValue = "createdAt") String sortBy) {

        SecurityUtils.UserContext ctx = SecurityUtils.getCurrentUser();
        Page<ExpenseResponse> expenses = expenseService.getMyExpenses(
                ctx.getUuid(), status, page, size, sortBy);
        return ResponseEntity.ok(ApiResponse.success("Expenses fetched", expenses));
    }

    @GetMapping
    @Operation(summary = "Get all expenses (FINANCE_ADMIN / MANAGER only, filterable)")
    @PreAuthorize("hasAnyRole('MANAGER','FINANCE_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Page<ExpenseResponse>>> getAllExpenses(
            @RequestParam(required = false) ExpenseStatus status,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<ExpenseResponse> expenses = expenseService.getAllExpenses(
                status, department, categoryId, from, to, page, size);
        return ResponseEntity.ok(ApiResponse.success("Expenses fetched", expenses));
    }

    @GetMapping("/search")
    @Operation(summary = "Search my expenses by keyword")
    public ResponseEntity<ApiResponse<Page<ExpenseResponse>>> searchExpenses(
            @RequestParam String query,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {

        SecurityUtils.UserContext ctx = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.success("Search results",
                expenseService.searchExpenses(ctx.getUuid(), query, page, size)));
    }

    @GetMapping("/{uuid}/audit")
    @Operation(summary = "Get audit trail for an expense")
    public ResponseEntity<ApiResponse<List<ExpenseResponse>>> getAuditTrail(
            @PathVariable String uuid) {

        SecurityUtils.UserContext ctx = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.success("Audit trail fetched",
                expenseService.getExpenseAuditTrail(uuid, ctx)));
    }

    // ─── UPDATE ───────────────────────────────────────────────

    @PutMapping("/{uuid}")
    @Operation(summary = "Update a DRAFT expense")
    public ResponseEntity<ApiResponse<ExpenseResponse>> updateExpense(
            @PathVariable String uuid,
            @Valid @RequestBody UpdateExpenseRequest request) {

        SecurityUtils.UserContext ctx = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.success("Expense updated",
                expenseService.updateExpense(uuid, request, ctx)));
    }

    // ─── SUBMIT ───────────────────────────────────────────────

    @PostMapping("/{uuid}/submit")
    @Operation(summary = "Submit a DRAFT expense for approval")
    public ResponseEntity<ApiResponse<ExpenseResponse>> submitExpense(
            @PathVariable String uuid) {

        SecurityUtils.UserContext ctx = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.success("Expense submitted for approval",
                expenseService.submitExpense(uuid, ctx)));
    }

    // ─── RECEIPT UPLOAD ───────────────────────────────────────

    @PostMapping(value = "/{uuid}/receipt", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload receipt (JPEG/PNG/PDF, max 10MB)")
    public ResponseEntity<ApiResponse<ExpenseResponse>> uploadReceipt(
            @PathVariable String uuid,
            @RequestParam("file") MultipartFile file) {

        SecurityUtils.UserContext ctx = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.success("Receipt uploaded successfully",
                expenseService.uploadReceipt(uuid, file, ctx)));
    }

    // ─── DELETE ───────────────────────────────────────────────

    @DeleteMapping("/{uuid}")
    @Operation(summary = "Soft-delete a DRAFT expense")
    public ResponseEntity<ApiResponse<Void>> deleteExpense(@PathVariable String uuid) {
        SecurityUtils.UserContext ctx = SecurityUtils.getCurrentUser();
        expenseService.deleteExpense(uuid, ctx);
        return ResponseEntity.ok(ApiResponse.success("Expense deleted", null));
    }

    // ─── INTERNAL (called by approval-service via REST) ───────

    @PatchMapping("/{uuid}/approve")
    @Operation(summary = "[Internal] Mark expense as APPROVED")
    @PreAuthorize("hasAnyRole('MANAGER','FINANCE_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Void> markApproved(
            @PathVariable String uuid,
            @RequestParam String approverEmail,
            @RequestParam String approverName) {

        expenseService.markApproved(uuid, approverEmail, approverName);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{uuid}/reject")
    @Operation(summary = "[Internal] Mark expense as REJECTED")
    @PreAuthorize("hasAnyRole('MANAGER','FINANCE_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Void> markRejected(
            @PathVariable String uuid,
            @RequestParam String approverEmail,
            @RequestParam String approverName,
            @RequestParam String reason) {

        expenseService.markRejected(uuid, approverEmail, approverName, reason);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{uuid}/reimburse")
    @Operation(summary = "Mark approved expense as REIMBURSED (FINANCE_ADMIN only)")
    @PreAuthorize("hasAnyRole('FINANCE_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> markReimbursed(
            @PathVariable String uuid) {

        SecurityUtils.UserContext ctx = SecurityUtils.getCurrentUser();
        expenseService.markReimbursed(uuid, ctx.getEmail(), ctx.getFullName());
        return ResponseEntity.ok(ApiResponse.success("Expense marked as reimbursed", null));
    }
}
