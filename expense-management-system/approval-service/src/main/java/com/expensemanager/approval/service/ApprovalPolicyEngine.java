package com.expensemanager.approval.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Determines how many approval levels are needed based on amount.
 *
 * Rules (configurable via application.yml):
 *   amount <= auto-approve-below  → auto-approve (0 levels)
 *   amount <= level-1-threshold   → 1 level (Manager only)
 *   amount <= level-2-threshold   → 2 levels (Manager + Finance)
 *   amount >  level-2-threshold   → 2 levels (Manager + Finance — same, both required)
 */
@Component
public class ApprovalPolicyEngine {

    @Value("${approval.policy.auto-approve-below:500}")
    private BigDecimal autoApproveBelow;

    @Value("${approval.policy.level-1-threshold:5000}")
    private BigDecimal level1Threshold;

    @Value("${approval.policy.level-2-threshold:25000}")
    private BigDecimal level2Threshold;

    public int getRequiredLevels(BigDecimal amount) {
        if (amount.compareTo(autoApproveBelow) <= 0) return 0;   // auto-approve
        if (amount.compareTo(level1Threshold)  <= 0) return 1;   // manager only
        return 2;                                                  // manager + finance
    }

    public boolean isAutoApprove(BigDecimal amount) {
        return amount.compareTo(autoApproveBelow) <= 0;
    }

    public String getLevelDescription(int level) {
        return switch (level) {
            case 1 -> "Manager Approval";
            case 2 -> "Finance Admin Approval";
            default -> "Unknown Level";
        };
    }
}
