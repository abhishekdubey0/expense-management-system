package com.expensemanager.approval.kafka;

import com.expensemanager.approval.service.ApprovalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExpenseSubmittedConsumer {

    private final ApprovalService approvalService;

    @KafkaListener(
        topics       = "${kafka.topics.expense-submitted:expense-submitted}",
        groupId      = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(@Payload ExpenseEvent event,
                        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                        @Header(KafkaHeaders.OFFSET) long offset) {
        log.info("Received [{}] from topic={} partition={} offset={}",
                event.getEventType(), topic, partition, offset);

        try {
            if ("EXPENSE_SUBMITTED".equals(event.getEventType())) {
                approvalService.createApprovalRequest(event);
            }
        } catch (Exception e) {
            log.error("Error processing expense event {}: {}",
                    event.getExpenseUuid(), e.getMessage(), e);
            // In production: send to dead-letter topic
        }
    }
}
