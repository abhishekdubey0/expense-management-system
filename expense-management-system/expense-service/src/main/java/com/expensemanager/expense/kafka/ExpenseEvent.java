package com.expensemanager.expense.kafka;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Published to Kafka when an expense status changes.
 * Consumed by: notification-service, report-service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseEvent {

    private String eventType;       // EXPENSE_SUBMITTED, EXPENSE_APPROVED, EXPENSE_REJECTED
    private String expenseUuid;
    private String expenseTitle;
    private BigDecimal amount;
    private String currency;
    private String status;

    private String submitterId;
    private String submitterEmail;
    private String submitterName;

    private String performedByEmail;
    private String performedByName;

    private String rejectionReason;
    private String department;

    @Builder.Default
    private LocalDateTime eventTime = LocalDateTime.now();
}
