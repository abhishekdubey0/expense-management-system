package com.expensemanager.expense.util;

import com.expensemanager.expense.dto.response.CategoryResponse;
import com.expensemanager.expense.dto.response.ExpenseResponse;
import com.expensemanager.expense.entity.Category;
import com.expensemanager.expense.entity.Expense;
import com.expensemanager.expense.entity.ExpenseAuditLog;
import com.expensemanager.expense.s3.S3StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExpenseMapper {

    private final S3StorageService s3StorageService;

    public ExpenseResponse toResponse(Expense expense) {
        if (expense == null) return null;
        return ExpenseResponse.builder()
                .id(expense.getId())
                .uuid(expense.getUuid())
                .title(expense.getTitle())
                .description(expense.getDescription())
                .amount(expense.getAmount())
                .currency(expense.getCurrency())
                .expenseDate(expense.getExpenseDate())
                .category(toCategory(expense.getCategory()))
                .status(expense.getStatus().name())
                .submitterId(expense.getSubmitterId())
                .submitterEmail(expense.getSubmitterEmail())
                .submitterName(expense.getSubmitterName())
                .department(expense.getDepartment())
                .projectCode(expense.getProjectCode())
                // Generate presigned URL at read time — never expose raw S3 key
                .receiptUrl(s3StorageService.generatePresignedUrl(expense.getReceiptUrl()))
                .receiptName(expense.getReceiptName())
                .notes(expense.getNotes())
                .rejectionReason(expense.getRejectionReason())
                .createdAt(expense.getCreatedAt())
                .updatedAt(expense.getUpdatedAt())
                .submittedAt(expense.getSubmittedAt())
                .approvedAt(expense.getApprovedAt())
                .build();
    }

    public CategoryResponse toCategory(Category category) {
        if (category == null) return null;
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .icon(category.getIcon())
                .build();
    }

    public ExpenseResponse auditToResponse(ExpenseAuditLog log) {
        // Returns a simplified response with audit info embedded
        return ExpenseResponse.builder()
                .uuid(log.getExpense().getUuid())
                .status(log.getNewStatus())
                .notes("[" + log.getAction() + "] by " + log.getPerformedByName()
                        + (log.getComment() != null ? ": " + log.getComment() : ""))
                .createdAt(log.getCreatedAt())
                .build();
    }
}
