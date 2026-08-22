package com.saga.order.kafka;

public final class KafkaTopics {

    private KafkaTopics() {
    }

    public static final String ORDER_EVENTS = "order-events";
    public static final String ORDER_COMMANDS = "order-commands";
}
