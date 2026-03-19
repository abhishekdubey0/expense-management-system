package com.expensemanager.notification.service;

import com.expensemanager.notification.consumer.ExpenseEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final EmailService emailService;

    @Value("${notification.app-url:http://localhost:3000}")
    private String appUrl;

    public void handleExpenseEvent(ExpenseEvent event) {
        switch (event.getEventType()) {
            case "EXPENSE_SUBMITTED"     -> notifyExpenseSubmitted(event);
            case "EXPENSE_APPROVED"      -> notifyExpenseApproved(event);
            case "EXPENSE_REJECTED"      -> notifyExpenseRejected(event);
            default -> log.warn("Unknown event type: {}", event.getEventType());
        }
    }

    // ─── Submitted ────────────────────────────────────────────

    private void notifyExpenseSubmitted(ExpenseEvent event) {
        // Notify the submitter their expense is in review
        emailService.sendHtmlEmail(
                event.getSubmitterEmail(),
                "Expense Submitted for Approval – " + event.getExpenseTitle(),
                "expense-submitted",
                Map.of(
                    "submitterName",  event.getSubmitterName(),
                    "expenseTitle",   event.getExpenseTitle(),
                    "amount",         formatAmount(event),
                    "expenseUuid",    event.getExpenseUuid(),
                    "appUrl",         appUrl
                )
        );
        log.info("Submission notification sent to {}", event.getSubmitterEmail());
    }

    // ─── Approved ─────────────────────────────────────────────

    private void notifyExpenseApproved(ExpenseEvent event) {
        emailService.sendHtmlEmail(
                event.getSubmitterEmail(),
                "✅ Your Expense Has Been Approved – " + event.getExpenseTitle(),
                "expense-approved",
                Map.of(
                    "submitterName",  event.getSubmitterName(),
                    "expenseTitle",   event.getExpenseTitle(),
                    "amount",         formatAmount(event),
                    "approverName",   event.getPerformedByName(),
                    "expenseUuid",    event.getExpenseUuid(),
                    "appUrl",         appUrl
                )
        );
        log.info("Approval notification sent to {}", event.getSubmitterEmail());
    }

    // ─── Rejected ─────────────────────────────────────────────

    private void notifyExpenseRejected(ExpenseEvent event) {
        emailService.sendHtmlEmail(
                event.getSubmitterEmail(),
                "❌ Your Expense Was Rejected – " + event.getExpenseTitle(),
                "expense-rejected",
                Map.of(
                    "submitterName",   event.getSubmitterName(),
                    "expenseTitle",    event.getExpenseTitle(),
                    "amount",          formatAmount(event),
                    "approverName",    event.getPerformedByName(),
                    "rejectionReason", event.getRejectionReason() != null
                                            ? event.getRejectionReason() : "No reason provided",
                    "expenseUuid",     event.getExpenseUuid(),
                    "appUrl",          appUrl
                )
        );
        log.info("Rejection notification sent to {}", event.getSubmitterEmail());
    }

    private String formatAmount(ExpenseEvent event) {
        return event.getCurrency() + " " + event.getAmount().toPlainString();
    }
}
