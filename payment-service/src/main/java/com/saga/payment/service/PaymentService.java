package com.saga.payment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saga.payment.dto.PaymentResponse;
import com.saga.payment.entity.OutboxEvent;
import com.saga.payment.entity.Payment;
import com.saga.payment.entity.enums.PaymentStatus;
import com.saga.payment.exception.PaymentNotFoundException;
import com.saga.payment.kafka.PaymentCommandPayload;
import com.saga.payment.kafka.PaymentEventPayload;
import com.saga.payment.kafka.PaymentEventType;
import com.saga.payment.repo.OutboxEventRepository;
import com.saga.payment.repo.PaymentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final ObjectMapper objectMapper;
    private final OutboxEventRepository outboxEventRepository;


    @Transactional
    public void processPayment(PaymentCommandPayload payload) {
        try {
            Payment payment = new Payment();
            payment.setOrderId(payload.orderId());
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setAmount(payload.totalAmount());
            payment.setPaymentMethod("ONLINE");
            paymentRepository.save(payment);

            publishEvent(payload.orderId(), payload.sagaId(), PaymentEventType.PAYMENT_PROCESSED, null);
            log.info("Payment Successfully Completed for order {}, (sagaId={})", payload.orderId(), payload.sagaId());

        } catch (Exception e) {
            publishFailureEvent(payload, e.getMessage());
        }
    }

    private void publishFailureEvent(PaymentCommandPayload command, String reason) {
        publishEvent(command.orderId(), command.sagaId(), PaymentEventType.PAYMENT_FAILED, reason);
        log.warn("Payment failed for order {} (sagaId={}): {}", command.orderId(), command.sagaId(), reason);
    }

    private void publishEvent(UUID orderId, UUID sagaId, String eventType, String reason) {
        PaymentEventPayload payload = new PaymentEventPayload(sagaId, orderId, eventType, reason, LocalDateTime.now());
        try{
            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateType("PAYMENT")
                    .aggregateId(orderId)
                    .eventType(eventType)
                    .payload(objectMapper.writeValueAsString(payload))
                    .processed(false)
                    .build();

                    outboxEventRepository.save(outboxEvent);
        }catch(JsonProcessingException e){
            throw new IllegalStateException("Failed to serialize outbox payload for order "+ orderId, e);
        }
    }

    @Transactional
    public void reversePayment(PaymentCommandPayload payload) {
        try {
            Payment payment = paymentRepository.findByOrderId(payload.orderId());
            if (payment == null) {
                log.error("No payment found to reverse for order {}", payload.orderId());
                return;
            }
            payment.setStatus(PaymentStatus.REFUNDED);
            paymentRepository.save(payment);
            publishEvent(payload.orderId(), payload.sagaId(), PaymentEventType.PAYMENT_REVERSED, null);
            log.info("Payment Success Fully Returned for the order {}, (sagaId={})", payload.orderId(), payload.sagaId());
        } catch (Exception e) {
            log.error("There is some issue in payment refund {}", e.getMessage());
        }
    }

    public PaymentResponse getPaymentByOrderId(UUID orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId);
        if (payment == null) {
            throw new PaymentNotFoundException("No payment found for order: " + orderId);
        }
        return PaymentResponse.from(payment);
    }
}
