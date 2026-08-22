package com.saga.payment.outbox;

import com.saga.payment.entity.OutboxEvent;
import com.saga.payment.kafka.KafkaTopics;
import com.saga.payment.repo.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Component
@Slf4j
@RequiredArgsConstructor
public class OutboxPublisher {

    private final OutboxEventRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelay = 500)
    public void publishEvent() {
        List<OutboxEvent> events = repository.findTop50ByProcessedFalseOrderByCreatedAtAsc();
        for (OutboxEvent event : events) {
            publishSingleEvent(event);
        }
    }

    private void publishSingleEvent(OutboxEvent event) {
        String key = event.getAggregateId().toString();

        CompletableFuture<?> future = kafkaTemplate.send(KafkaTopics.PAYMENT_EVENTS, key, event.getPayload());
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                markProcessed(event.getId());
                log.info("Published outbox event for Inventory event [{}] eventType={} aggregateId={}", event.getId(), event.getEventType(), event.getAggregateType());
            } else {
                log.error("Failed to publish inventory outbox event [{}], will retry on next poll", event.getId(), ex);
            }
        });
    }

    private void markProcessed(UUID id) {
        repository.findById(id).ifPresent(e -> {
            e.setProcessed(true);
            repository.save(e);
        });
    }

}
