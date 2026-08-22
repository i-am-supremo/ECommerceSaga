package com.saga.order.kafka;

public final class OrderEventType {

    private OrderEventType() {
    }

    public static final String ORDER_CREATED = "OrderCreated";
    public static final String ORDER_CONFIRMED = "OrderConfirmed";
    public static final String ORDER_CANCELLED = "OrderCancelled";

    public static final String CONFIRM_ORDER = "ConfirmOrder";
    public static final String CANCEL_ORDER = "CancelOrder";
}
