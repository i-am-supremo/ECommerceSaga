package com.saga.orchestrator.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saga.orchestrator.entity.OutboxEvent;
import com.saga.orchestrator.entity.SagaInstance;
import com.saga.orchestrator.entity.SagaStep;
import com.saga.orchestrator.entity.enums.SagaStatus;
import com.saga.orchestrator.entity.enums.StepStatus;
import com.saga.orchestrator.exception.SagaNotFoundException;
import com.saga.orchestrator.kafka.*;
import com.saga.orchestrator.repo.OutboxEventRepository;
import com.saga.orchestrator.repo.SagaInstanceRepository;
import com.saga.orchestrator.repo.SagaStepRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SagaOrchestratorService {

    private static final String STEP_RESERVE_INVENTORY = "RESERVE_INVENTORY";
    private static final String STEP_PROCESS_PAYMENT = "PROCESS_PAYMENT";
    private static final String STEP_CONFIRM_ORDER = "CONFIRM_ORDER";

    private final SagaInstanceRepository sagaInstanceRepository;
    private final SagaStepRepository sagaStepRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    // ============ STEP 1: Order created -> reserve inventory ============

    @Transactional
    public void startSaga(OrderEventPayload event) {
        SagaInstance saga = SagaInstance.builder()
                .orderId(event.orderId())
                .customerId(event.customerId())
                .totalAmount(event.totalAmount())
                .currentStep(STEP_RESERVE_INVENTORY)
                .status(SagaStatus.STARTED)
                .build();
        saga = sagaInstanceRepository.save(saga); // sagaId generated here

        createStep(saga.getSagaId(), STEP_RESERVE_INVENTORY);

        List<Item> items = event.items().stream()
                .map(i -> new Item(i.productId(), i.quantity()))
                .collect(Collectors.toList());

        InventoryCommandPayload command = new InventoryCommandPayload(
                saga.getSagaId(), event.orderId(), SagaCommandType.RESERVE_INVENTORY, items);

        saveCommand(saga.getSagaId(), KafkaTopics.INVENTORY_COMMANDS, SagaCommandType.RESERVE_INVENTORY, command);

        log.info("Saga {} started for order {} -> ReserveInventory sent", saga.getSagaId(), event.orderId());
    }

    // ============ STEP 2: Inventory result -> process payment OR compensate ============

    @Transactional
    public void handleInventoryReserved(InventoryEventPayload event) {
        SagaInstance saga = getSagaOrThrow(event.sagaId());
        completeStep(saga.getSagaId(), STEP_RESERVE_INVENTORY, StepStatus.SUCCESS);

        saga.setCurrentStep(STEP_PROCESS_PAYMENT);
        saga.setStatus(SagaStatus.IN_PROGRESS);
        sagaInstanceRepository.save(saga);

        createStep(saga.getSagaId(), STEP_PROCESS_PAYMENT);

        PaymentCommandPayload command = new PaymentCommandPayload(
                saga.getSagaId(), saga.getOrderId(), SagaCommandType.PROCESS_PAYMENT, saga.getTotalAmount());

        saveCommand(saga.getSagaId(), KafkaTopics.PAYMENT_COMMANDS, SagaCommandType.PROCESS_PAYMENT, command);

        log.info("Saga {} -> inventory reserved, ProcessPayment sent", saga.getSagaId());
    }

    @Transactional
    public void handleInventoryReservationFailed(InventoryEventPayload event) {
        SagaInstance saga = getSagaOrThrow(event.sagaId());
        completeStep(saga.getSagaId(), STEP_RESERVE_INVENTORY, StepStatus.FAILED);

        // Nothing was reserved, so no inventory release needed - just cancel the order.
        saga.setStatus(SagaStatus.COMPENSATING);
        saga.setInventoryCompensated(true); // nothing to compensate here, mark done immediately
        sagaInstanceRepository.save(saga);

        sendCancelOrder(saga);

        log.warn("Saga {} -> inventory reservation failed ({}), cancelling order", saga.getSagaId(), event.reason());
    }

    // Response to OUR OWN ReleaseInventory compensation command (fired from handlePaymentFailed below)
    @Transactional
    public void handleInventoryReleased(InventoryEventPayload event) {
        SagaInstance saga = getSagaOrThrow(event.sagaId());
        completeStep(saga.getSagaId(), STEP_RESERVE_INVENTORY, StepStatus.COMPENSATED);

        saga.setInventoryCompensated(true);
        sagaInstanceRepository.save(saga);

        maybeMarkFullyCompensated(saga);

        log.info("Saga {} -> inventory released (compensation)", saga.getSagaId());
    }

    // ============ STEP 3: Payment result -> confirm order OR compensate ============

    @Transactional
    public void handlePaymentProcessed(PaymentEventPayload event) {
        SagaInstance saga = getSagaOrThrow(event.sagaId());
        completeStep(saga.getSagaId(), STEP_PROCESS_PAYMENT, StepStatus.SUCCESS);

        saga.setCurrentStep(STEP_CONFIRM_ORDER);
        sagaInstanceRepository.save(saga);

        createStep(saga.getSagaId(), STEP_CONFIRM_ORDER);

        OrderCommandPayload confirmCommand = new OrderCommandPayload(
                saga.getSagaId(), saga.getOrderId(), SagaCommandType.CONFIRM_ORDER);
        saveCommand(saga.getSagaId(), KafkaTopics.ORDER_COMMANDS, SagaCommandType.CONFIRM_ORDER, confirmCommand);

        // Fire-and-forget - not part of the compensation-tracked critical path
        NotificationCommandPayload notifyCommand = new NotificationCommandPayload(
                saga.getSagaId(), saga.getOrderId(), SagaCommandType.SEND_NOTIFICATION,
                "Your order has been confirmed!");
        saveCommand(saga.getSagaId(), KafkaTopics.NOTIFICATION_COMMANDS, SagaCommandType.SEND_NOTIFICATION, notifyCommand);

        log.info("Saga {} -> payment processed, ConfirmOrder + SendNotification sent", saga.getSagaId());
    }

    @Transactional
    public void handlePaymentFailed(PaymentEventPayload event) {
        SagaInstance saga = getSagaOrThrow(event.sagaId());
        completeStep(saga.getSagaId(), STEP_PROCESS_PAYMENT, StepStatus.FAILED);

        saga.setStatus(SagaStatus.COMPENSATING);
        sagaInstanceRepository.save(saga);

        // Two compensating actions run independently - inventory release + order cancel
        List<Item> items = List.of(); // orchestrator doesn't need item details to release -
        // Inventory Service looks up its own RESERVED reservations by orderId internally.
        InventoryCommandPayload releaseCommand = new InventoryCommandPayload(
                saga.getSagaId(), saga.getOrderId(), SagaCommandType.RELEASE_INVENTORY, items);
        saveCommand(saga.getSagaId(), KafkaTopics.INVENTORY_COMMANDS, SagaCommandType.RELEASE_INVENTORY, releaseCommand);

        sendCancelOrder(saga);

        log.warn("Saga {} -> payment failed ({}), releasing inventory + cancelling order",
                saga.getSagaId(), event.reason());
    }

    // ============ STEP 4: Order confirmation/cancellation acknowledgements ============

    @Transactional
    public void handleOrderConfirmed(OrderEventPayload event) {
        if (event.sagaId() == null) return; // safety guard, shouldn't happen for CONFIRMED
        SagaInstance saga = getSagaOrThrow(event.sagaId());
        completeStep(saga.getSagaId(), STEP_CONFIRM_ORDER, StepStatus.SUCCESS);

        saga.setStatus(SagaStatus.COMPLETED);
        sagaInstanceRepository.save(saga);

        log.info("Saga {} COMPLETED for order {}", saga.getSagaId(), event.orderId());
    }

    @Transactional
    public void handleOrderCancelled(OrderEventPayload event) {
        if (event.sagaId() == null) return;
        SagaInstance saga = getSagaOrThrow(event.sagaId());

        saga.setOrderCompensated(true);
        sagaInstanceRepository.save(saga);

        maybeMarkFullyCompensated(saga);

        log.info("Saga {} -> order cancelled (compensation)", saga.getSagaId());
    }

    // ============ helpers ============

    private void sendCancelOrder(SagaInstance saga) {
        OrderCommandPayload cancelCommand = new OrderCommandPayload(
                saga.getSagaId(), saga.getOrderId(), SagaCommandType.CANCEL_ORDER);
        saveCommand(saga.getSagaId(), KafkaTopics.ORDER_COMMANDS, SagaCommandType.CANCEL_ORDER, cancelCommand);
    }

    private void maybeMarkFullyCompensated(SagaInstance saga) {
        if (Boolean.TRUE.equals(saga.getInventoryCompensated()) && Boolean.TRUE.equals(saga.getOrderCompensated())) {
            saga.setStatus(SagaStatus.COMPENSATED);
            sagaInstanceRepository.save(saga);
            log.info("Saga {} fully COMPENSATED", saga.getSagaId());
        }
    }

    private void createStep(UUID sagaId, String stepName) {
        SagaStep step = SagaStep.builder()
                .sagaId(sagaId)
                .stepName(stepName)
                .status(StepStatus.PENDING)
                .build();
        sagaStepRepository.save(step);
    }

    private void completeStep(UUID sagaId, String stepName, StepStatus status) {
        sagaStepRepository.findBySagaIdAndStepName(sagaId, stepName).ifPresent(step -> {
            step.setStatus(status);
            step.setCompletedAt(java.time.LocalDateTime.now());
            sagaStepRepository.save(step);
        });
    }

    private SagaInstance getSagaOrThrow(UUID sagaId) {
        return sagaInstanceRepository.findById(sagaId)
                .orElseThrow(() -> new SagaNotFoundException("Saga not found: " + sagaId));
    }

    /**
     * Generic outbox writer for any command payload, going to any target topic.
     * This is the ONE place that guarantees "saga state update + command dispatch"
     * happen in the same DB transaction as whatever method called this.
     */
    private void saveCommand(UUID sagaId, String targetTopic, String commandType, Object payload) {
        try {
            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateType("SAGA")
                    .aggregateId(sagaId)
                    .eventType(commandType)
                    .targetTopic(targetTopic)
                    .payload(objectMapper.writeValueAsString(payload))
                    .processed(false)
                    .build();
            outboxEventRepository.save(outboxEvent);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize command payload for saga " + sagaId, e);
        }
    }
}