package com.saga.order.kafka;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record OrderEventPayload(
        UUID orderId,
        UUID sagaId,
        UUID customerId,
        BigDecimal totalAmount,
        List<Item> items,
        String status,
        LocalDateTime timestamp
) {
    public record Item(UUID productId, Integer quantity, BigDecimal unitPrice) {
    }
}