package com.expensemanager.notification.consumer;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ExpenseEvent {
    private String       eventType;
    private String       expenseUuid;
    private String       expenseTitle;
    private BigDecimal   amount;
    private String       currency;
    private String       status;
    private String       submitterId;
    private String       submitterEmail;
    private String       submitterName;
    private String       performedByEmail;
    private String       performedByName;
    private String       rejectionReason;
    private String       department;
    private LocalDateTime eventTime;
}
