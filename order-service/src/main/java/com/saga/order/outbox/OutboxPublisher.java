package com.saga.order.outbox;

import com.saga.order.entity.OutboxEvent;
import com.saga.order.kafka.KafkaTopics;
import com.saga.order.repo.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
        List<OutboxEvent> pendingEvents = outboxEventRepository.findTop50ByProcessedFalseOrderByCreatedAtAsc();

        for (OutboxEvent event : pendingEvents) {
            publishSingleEvent(event);
        }
    }

    private void publishSingleEvent(OutboxEvent event) {
        String key = event.getAggregateId().toString();

        CompletableFuture<?> future = kafkaTemplate.send(KafkaTopics.ORDER_EVENTS, key, event.getPayload());

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                markProcessed(event.getId());
                log.info("Published outbox event [{}] eventType={} aggregateId={}",
                        event.getId(), event.getEventType(), event.getAggregateId());
            } else {
                log.error("Failed to publish outbox event [{}], will retry on next poll", event.getId(), ex);
            }
        });
    }

    @Transactional
    protected void markProcessed(UUID eventId) {
        outboxEventRepository.findById(eventId).ifPresent(e -> {
            e.setProcessed(true);
            outboxEventRepository.save(e);
        });
    }
}