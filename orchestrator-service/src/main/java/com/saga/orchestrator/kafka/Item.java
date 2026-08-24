package com.saga.orchestrator.kafka;

import java.util.UUID;

public record Item(UUID productId, Integer quantity) {
}
