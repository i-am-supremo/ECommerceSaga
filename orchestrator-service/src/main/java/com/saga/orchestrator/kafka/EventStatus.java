package com.saga.orchestrator.kafka;

public final class EventStatus {
    private EventStatus() {
    }

    // order-events -> OrderStatus enum name as-is
    public static final String ORDER_CREATED = "CREATED";
    public static final String ORDER_CONFIRMED = "CONFIRMED";
    public static final String ORDER_CANCELLED = "CANCELLED";

    // inventory-events -> short custom strings
    public static final String INVENTORY_RESERVED = "RESERVED";
    public static final String INVENTORY_FAILED = "FAILED";
    public static final String INVENTORY_RELEASED = "RELEASED";

    // payment-events -> full event-type-style strings
    public static final String PAYMENT_PROCESSED = "PaymentProcessed";
    public static final String PAYMENT_FAILED = "PaymentFailed";
    public static final String PAYMENT_REVERSED = "PaymentReversed";
}
