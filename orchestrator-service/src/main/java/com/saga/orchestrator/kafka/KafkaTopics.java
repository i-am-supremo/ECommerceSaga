package com.saga.orchestrator.kafka;

public final class KafkaTopics {
    private KafkaTopics() {
    }

    // We CONSUME these (facts from each service)
    public static final String ORDER_EVENTS = "order-events";
    public static final String INVENTORY_EVENTS = "inventory-events";
    public static final String PAYMENT_EVENTS = "payment-events";

    // We PRODUCE these (instructions to each service)
    public static final String ORDER_COMMANDS = "order-commands";
    public static final String INVENTORY_COMMANDS = "inventory-commands";
    public static final String PAYMENT_COMMANDS = "payment-commands";
    public static final String NOTIFICATION_COMMANDS = "notification-commands";

}
