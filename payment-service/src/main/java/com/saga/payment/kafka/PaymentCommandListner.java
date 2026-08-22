package com.saga.payment.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saga.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentCommandListner {

    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = KafkaTopics.PAYMENT_COMMANDS)
    public void handleCommand(String message){
        try{
            PaymentCommandPayload payload = objectMapper.readValue(message, PaymentCommandPayload.class);
            log.info("Received inventory command {} for order {} (sagaId={})", payload.commandType(), payload.orderId(), payload.sagaId());

            switch (payload.commandType()){
                case PaymentEventType.PROCESS_PAYMENT -> paymentService.processPayment(payload);
                case PaymentEventType.REVERSE_PAYMENT -> paymentService.reversePayment(payload);
                default -> log.warn("Invalid command received in Payment {}", payload.commandType());
            }

        }catch(Exception e){
            log.error("Error while processing command {}", message, e);
        }
    }
}
