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
public class OrderEventListener {

    private final SagaOrchestratorService sagaOrchestratorService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = KafkaTopics.ORDER_EVENTS, groupId = "saga-orchestrator-service")
    public void handleEvent(String message) {
        try {
            OrderEventPayload event = objectMapper.readValue(message, OrderEventPayload.class);
            log.info("Received order event status={} for order {}", event.status(), event.orderId());

            switch (event.status()) {
                case EventStatus.ORDER_CREATED -> sagaOrchestratorService.startSaga(event);
                case EventStatus.ORDER_CONFIRMED -> sagaOrchestratorService.handleOrderConfirmed(event);
                case EventStatus.ORDER_CANCELLED -> sagaOrchestratorService.handleOrderCancelled(event);
                default -> log.warn("Unhandled order status: {}", event.status());
            }
        } catch (Exception e) {
            log.error("Failed to process order event message: {}", message, e);
        }
    }
}
