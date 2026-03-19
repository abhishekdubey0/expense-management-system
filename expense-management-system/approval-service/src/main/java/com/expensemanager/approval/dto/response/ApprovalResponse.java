package com.expensemanager.approval.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApprovalResponse {
    private Long   id;
    private String uuid;
    private String expenseUuid;
    private String expenseTitle;
    private BigDecimal amount;
    private String currency;
    private String submitterName;
    private String submitterEmail;
    private String department;
    private String status;
    private Integer currentLevel;
    private Integer totalLevels;
    private List<ApprovalActionResponse> actions;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}
