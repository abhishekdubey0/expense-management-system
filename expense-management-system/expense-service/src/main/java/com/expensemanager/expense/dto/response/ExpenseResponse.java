package com.expensemanager.expense.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExpenseResponse {
    private Long id;
    private String uuid;
    private String title;
    private String description;
    private BigDecimal amount;
    private String currency;
    private LocalDate expenseDate;
    private CategoryResponse category;
    private String status;
    private String submitterId;
    private String submitterEmail;
    private String submitterName;
    private String department;
    private String projectCode;
    private String receiptUrl;          // Presigned S3 URL (not raw key)
    private String receiptName;
    private String notes;
    private String rejectionReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime submittedAt;
    private LocalDateTime approvedAt;
}
