package com.expensemanager.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Circuit breaker fallback — returned when a downstream service is unavailable.
 * These endpoints are forwarded to internally by the gateway when a CB trips.
 */
@RestController
@RequestMapping("/fallback")
@Slf4j
public class FallbackController {

    @GetMapping("/auth")
    public ResponseEntity<Map<String, Object>> authFallback() {
        log.warn("Circuit breaker open: auth-service unavailable");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "success", false,
                "error",   "Auth service is temporarily unavailable. Please try again shortly.",
                "service", "auth-service"
        ));
    }

    @GetMapping("/expense")
    public ResponseEntity<Map<String, Object>> expenseFallback() {
        log.warn("Circuit breaker open: expense-service unavailable");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "success", false,
                "error",   "Expense service is temporarily unavailable. Please try again shortly.",
                "service", "expense-service"
        ));
    }

    @GetMapping("/approval")
    public ResponseEntity<Map<String, Object>> approvalFallback() {
        log.warn("Circuit breaker open: approval-service unavailable");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "success", false,
                "error",   "Approval service is temporarily unavailable. Please try again shortly.",
                "service", "approval-service"
        ));
    }

    @GetMapping("/report")
    public ResponseEntity<Map<String, Object>> reportFallback() {
        log.warn("Circuit breaker open: report-service unavailable");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "success", false,
                "error",   "Report service is temporarily unavailable. Please try again shortly.",
                "service", "report-service"
        ));
    }
}
