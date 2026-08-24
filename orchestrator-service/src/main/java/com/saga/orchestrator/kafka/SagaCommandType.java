package com.saga.orchestrator.kafka;

public final class SagaCommandType {

    private SagaCommandType() {
    }

    public static final String RESERVE_INVENTORY = "ReserveInventory";
    public static final String RELEASE_INVENTORY = "ReleaseInventory";

    public static final String PROCESS_PAYMENT = "ProcessPayment";
    public static final String REVERSE_PAYMENT = "ReversePayment";

    public static final String CONFIRM_ORDER = "ConfirmOrder";
    public static final String CANCEL_ORDER = "CancelOrder";

    public static final String SEND_NOTIFICATION = "SendNotification";
}
