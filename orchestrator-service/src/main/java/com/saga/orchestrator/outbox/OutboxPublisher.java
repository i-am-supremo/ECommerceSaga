package com.saga.orchestrator.outbox;

import com.saga.orchestrator.entity.OutboxEvent;
import com.saga.orchestrator.repo.OutboxEventRepository;
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

        // Unlike other services, topic comes from the row itself (targetTopic),
        // not a hardcoded constant - this is what lets one outbox table
        // fan out commands to 3 different downstream services.
        CompletableFuture<?> future = kafkaTemplate.send(event.getTargetTopic(), key, event.getPayload());

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                markProcessed(event.getId());
                log.info("Published command [{}] type={} -> topic={} sagaId={}",
                        event.getId(), event.getEventType(), event.getTargetTopic(), event.getAggregateId());
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
