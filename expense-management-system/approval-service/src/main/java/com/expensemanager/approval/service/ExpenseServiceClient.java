package com.expensemanager.approval.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Internal HTTP client from approval-service → expense-service.
 * Called to update expense status after approval/rejection.
 * Uses an internal service-to-service token (no user JWT needed).
 */
@Component
@Slf4j
public class ExpenseServiceClient {

    private final RestTemplate restTemplate;

    @Value("${expense-service.url:http://expense-service:8082}")
    private String expenseServiceUrl;

    public ExpenseServiceClient() {
        this.restTemplate = new RestTemplate();
    }

    public void markExpenseApproved(String expenseUuid,
                                     String approverEmail,
                                     String approverName) {
        String url = expenseServiceUrl + "/api/v1/internal/expenses/" + expenseUuid + "/approve";
        try {
            HttpHeaders headers = buildInternalHeaders();
            Map<String, String> body = Map.of(
                    "approverEmail", approverEmail,
                    "approverName",  approverName
            );
            restTemplate.exchange(url, HttpMethod.POST,
                    new HttpEntity<>(body, headers), Void.class);
            log.info("Expense {} marked approved via expense-service", expenseUuid);
        } catch (Exception e) {
            log.error("Failed to mark expense {} approved: {}", expenseUuid, e.getMessage());
            throw new RuntimeException("Failed to update expense status", e);
        }
    }

    public void markExpenseRejected(String expenseUuid,
                                     String approverEmail,
                                     String approverName,
                                     String reason) {
        String url = expenseServiceUrl + "/api/v1/internal/expenses/" + expenseUuid + "/reject";
        try {
            HttpHeaders headers = buildInternalHeaders();
            Map<String, String> body = Map.of(
                    "approverEmail",    approverEmail,
                    "approverName",     approverName,
                    "rejectionReason",  reason != null ? reason : ""
            );
            restTemplate.exchange(url, HttpMethod.POST,
                    new HttpEntity<>(body, headers), Void.class);
            log.info("Expense {} marked rejected via expense-service", expenseUuid);
        } catch (Exception e) {
            log.error("Failed to mark expense {} rejected: {}", expenseUuid, e.getMessage());
            throw new RuntimeException("Failed to update expense status", e);
        }
    }

    private HttpHeaders buildInternalHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // Internal service header — API gateway blocks this from external traffic
        headers.set("X-Internal-Service", "approval-service");
        return headers;
    }
}
