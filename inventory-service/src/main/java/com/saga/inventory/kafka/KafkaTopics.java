package com.saga.inventory.kafka;

public final class KafkaTopics {

    private KafkaTopics() {
    }

    public static final String INVENTORY_EVENTS = "inventory-events";
    public static final String INVENTORY_COMMANDS = "inventory-commands";
}
