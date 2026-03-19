package com.expensemanager.notification.consumer;

import com.expensemanager.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExpenseEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(
        topics = {
            "${kafka.topics.expense-submitted:expense-submitted}",
            "${kafka.topics.expense-status-changed:expense-status-changed}"
        },
        groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(@Payload ExpenseEvent event,
                        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                        @Header(KafkaHeaders.OFFSET) long offset) {
        log.info("Notification consumer received [{}] from {} offset={}",
                event.getEventType(), topic, offset);
        try {
            notificationService.handleExpenseEvent(event);
        } catch (Exception e) {
            log.error("Error processing notification for event [{}] expense [{}]: {}",
                    event.getEventType(), event.getExpenseUuid(), e.getMessage());
        }
    }
}
