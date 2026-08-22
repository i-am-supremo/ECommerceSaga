package com.saga.inventory.kafka;

public final class InventoryEventType {
    private InventoryEventType() {

    }

    // Events this service PUBLISHES
    public static final String INVENTORY_RESERVED = "InventoryReserved";
    public static final String INVENTORY_RESERVATION_FAILED = "InventoryReservationFailed";
    public static final String INVENTORY_RELEASED = "InventoryReleased";

    // Commands this service CONSUMES (from orchestrator)
    public static final String RESERVE_INVENTORY = "ReserveInventory";
    public static final String RELEASE_INVENTORY = "ReleaseInventory";
}
