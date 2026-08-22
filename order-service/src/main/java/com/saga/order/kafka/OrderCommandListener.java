package com.saga.order.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saga.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCommandListener {

    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = KafkaTopics.ORDER_COMMANDS, groupId = "order-service")
    public void handleCommand(String message) {
        try {
            OrderCommandPayload command = objectMapper.readValue(message, OrderCommandPayload.class);
            log.info("Received command {} for order {} (sagaId={})",
                    command.commandType(), command.orderId(), command.sagaId());

            switch (command.commandType()) {
                case OrderEventType.CONFIRM_ORDER ->
                        orderService.confirmOrder(command.orderId(), command.sagaId());
                case OrderEventType.CANCEL_ORDER ->
                        orderService.cancelOrder(command.orderId(), command.sagaId());
                default ->
                        log.warn("Unknown command type received: {}", command.commandType());
            }
        } catch (Exception e) {
            log.error("Failed to process order command message: {}", message, e);
        }
    }
}