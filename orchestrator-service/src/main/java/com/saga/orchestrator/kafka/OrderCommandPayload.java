package com.saga.orchestrator.kafka;

import java.util.UUID;

public record OrderCommandPayload(
        UUID sagaId,
        UUID orderId,
        String commandType
) {
}
