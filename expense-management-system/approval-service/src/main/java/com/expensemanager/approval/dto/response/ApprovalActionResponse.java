package com.expensemanager.approval.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApprovalActionResponse {
    private Integer level;
    private String  action;
    private String  approverName;
    private String  approverEmail;
    private String  approverRole;
    private String  comment;
    private LocalDateTime actedAt;
}
