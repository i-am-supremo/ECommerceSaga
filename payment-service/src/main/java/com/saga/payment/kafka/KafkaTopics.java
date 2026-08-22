package com.saga.payment.kafka;

public final class KafkaTopics {
    private KafkaTopics() {
    }

    public static final String PAYMENT_EVENTS = "payment-events";
    public static final String PAYMENT_COMMANDS = "payment-commands";
}
