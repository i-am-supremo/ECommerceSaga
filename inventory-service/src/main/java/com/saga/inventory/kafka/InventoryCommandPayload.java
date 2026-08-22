package com.saga.inventory.kafka;

import java.util.List;
import java.util.UUID;

public record InventoryCommandPayload(UUID sagaId, UUID orderId, String commandType, List<Item> items){}

