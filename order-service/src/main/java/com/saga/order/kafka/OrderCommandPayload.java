package com.saga.order.kafka;

import java.util.UUID;

public record OrderCommandPayload(
        UUID sagaId,
        UUID orderId,
        String commandType
) {
}
