package com.saga.orchestrator.kafka;

import java.util.UUID;

public record NotificationCommandPayload(
        UUID sagaId,
        UUID orderId,
        String commandType,
        String message
) {
}
