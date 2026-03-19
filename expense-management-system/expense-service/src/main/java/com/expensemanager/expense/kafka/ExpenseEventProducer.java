package com.expensemanager.expense.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExpenseEventProducer {

    private final KafkaTemplate<String, ExpenseEvent> kafkaTemplate;

    @Value("${kafka.topics.expense-submitted:expense-submitted}")
    private String expenseSubmittedTopic;

    @Value("${kafka.topics.expense-status-changed:expense-status-changed}")
    private String expenseStatusChangedTopic;

    @Async
    public void publishExpenseSubmitted(ExpenseEvent event) {
        publish(expenseSubmittedTopic, event.getExpenseUuid(), event);
    }

    @Async
    public void publishExpenseStatusChanged(ExpenseEvent event) {
        publish(expenseStatusChangedTopic, event.getExpenseUuid(), event);
    }

    private void publish(String topic, String key, ExpenseEvent event) {
        CompletableFuture<SendResult<String, ExpenseEvent>> future =
                kafkaTemplate.send(topic, key, event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish event [{}] to topic [{}]: {}",
                        event.getEventType(), topic, ex.getMessage());
            } else {
                log.info("Event [{}] published to topic [{}] partition [{}] offset [{}]",
                        event.getEventType(), topic,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }
}
