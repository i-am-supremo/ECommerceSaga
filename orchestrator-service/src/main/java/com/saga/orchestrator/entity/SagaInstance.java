package com.saga.orchestrator.entity;

import com.saga.orchestrator.entity.enums.SagaStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "saga_instances")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SagaInstance {

    @Id
    @GeneratedValue
    @Column(name = "saga_id")
    private UUID sagaId;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "customer_id")
    private UUID customerId;

    // Stored here because PaymentCommand needs it later, and Inventory/Payment
    // events don't carry it back to us - so we keep our own copy at saga start.
    @Column(name = "total_amount", precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "current_step", nullable = false, length = 50)
    private String currentStep; // RESERVE_INVENTORY, PROCESS_PAYMENT, CONFIRM_ORDER

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SagaStatus status;

    // Used only during compensation, to know when BOTH compensating actions
    // (inventory release + order cancel) have finished before marking
    // the saga fully COMPENSATED.
    @Builder.Default
    @Column(name = "inventory_compensated", nullable = false)
    private Boolean inventoryCompensated = false;

    @Builder.Default
    @Column(name = "order_compensated", nullable = false)
    private Boolean orderCompensated = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = SagaStatus.STARTED;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}