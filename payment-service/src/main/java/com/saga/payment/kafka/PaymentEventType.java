package com.saga.payment.kafka;

public class PaymentEventType {

    // Commands this service receives
    public static final String PROCESS_PAYMENT = "ProcessPayment";
    public static final String REVERSE_PAYMENT = "ReversePayment";

    // Commands this service produces
    public static final String PAYMENT_PROCESSED = "PaymentProcessed";
    public static final String PAYMENT_REVERSED = "PaymentReversed";
    public static final String PAYMENT_FAILED = "PaymentFailed";
}
