package com.saga.payment.kafka;

import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentEventPayload(
        UUID sagaId,
        UUID orderId,
        String status,
        String reason,
        LocalDateTime timestamp
) {
}
