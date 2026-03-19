package com.expensemanager.approval.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ApprovalDecisionRequest {

    @Size(max = 1000, message = "Comment cannot exceed 1000 characters")
    private String comment;
}
