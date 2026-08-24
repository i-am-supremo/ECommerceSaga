package com.saga.orchestrator.kafka;

import java.util.List;
import java.util.UUID;

public record InventoryCommandPayload(
        UUID sagaId,
        UUID orderId,
        String commandType,
        List<Item> items
) {
}
