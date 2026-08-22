package com.saga.inventory.kafka;

import java.util.UUID;

public record Item(UUID productId, Integer quantity){}
