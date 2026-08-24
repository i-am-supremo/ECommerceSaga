package com.saga.orchestrator.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saga.orchestrator.service.SagaOrchestratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryEventListener {

    private final SagaOrchestratorService sagaOrchestratorService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = KafkaTopics.INVENTORY_EVENTS, groupId = "saga-orchestrator-service")
    public void handleEvent(String message) {
        try {
            InventoryEventPayload event = objectMapper.readValue(message, InventoryEventPayload.class);
            log.info("Received inventory event status={} for order {} (sagaId={})",
                    event.status(), event.orderId(), event.sagaId());

            switch (event.status()) {
                case EventStatus.INVENTORY_RESERVED -> sagaOrchestratorService.handleInventoryReserved(event);
                case EventStatus.INVENTORY_FAILED -> sagaOrchestratorService.handleInventoryReservationFailed(event);
                case EventStatus.INVENTORY_RELEASED -> sagaOrchestratorService.handleInventoryReleased(event);
                default -> log.warn("Unhandled inventory status: {}", event.status());
            }
        } catch (Exception e) {
            log.error("Failed to process inventory event message: {}", message, e);
        }
    }
}
