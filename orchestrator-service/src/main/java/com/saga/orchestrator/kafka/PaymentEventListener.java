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
public class PaymentEventListener {

    private final SagaOrchestratorService sagaOrchestratorService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = KafkaTopics.PAYMENT_EVENTS, groupId = "saga-orchestrator-service")
    public void handleEvent(String message) {
        try {
            PaymentEventPayload event = objectMapper.readValue(message, PaymentEventPayload.class);
            log.info("Received payment event status={} for order {} (sagaId={})",
                    event.status(), event.orderId(), event.sagaId());

            switch (event.status()) {
                case EventStatus.PAYMENT_PROCESSED -> sagaOrchestratorService.handlePaymentProcessed(event);
                case EventStatus.PAYMENT_FAILED -> sagaOrchestratorService.handlePaymentFailed(event);
                case EventStatus.PAYMENT_REVERSED -> log.info("Payment reversed acknowledged for order {}", event.orderId());
                default -> log.warn("Unhandled payment status: {}", event.status());
            }
        } catch (Exception e) {
            log.error("Failed to process payment event message: {}", message, e);
        }
    }
}
