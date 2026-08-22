package com.saga.inventory.outbox;

import com.saga.inventory.entity.OutboxEvent;
import com.saga.inventory.kafka.KafkaTopics;
import com.saga.inventory.repo.InventoryRepository;
import com.saga.inventory.repo.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelay = 500)
    public void publishPendingEvents() {
        List<OutboxEvent> outboxEvents = outboxEventRepository.findTop50ByProcessedFalseOrderByCreatedAtAsc();
        for (OutboxEvent outboxEvent : outboxEvents) {
            publishSingleEvent(outboxEvent);
        }
    }

    private void publishSingleEvent(OutboxEvent outboxEvent) {
        String key = outboxEvent.getAggregateId().toString();

        CompletableFuture<?> future = kafkaTemplate.send(KafkaTopics.INVENTORY_EVENTS, key, outboxEvent.getPayload());
        future.whenComplete((result, ex) ->{
           if (ex == null) {
               markProcessed(outboxEvent.getId());
               log.info("Published outbox event for Inventory event [{}] eventType={} aggregateId={}", outboxEvent.getId(), outboxEvent.getEventType(), outboxEvent.getAggregateType());
           }
           else {
               log.error("Failed to publish inventory outbox event [{}], will retry on next poll", outboxEvent.getId(), ex);
           }
        });
    }

    private void markProcessed(UUID id) {
        outboxEventRepository.findById(id).ifPresent(e -> {
            e.setProcessed(true);
            outboxEventRepository.save(e);
        });
    }
}
