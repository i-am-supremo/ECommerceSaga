package com.saga.inventory.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "inventory")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Inventory {

    @Id
    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "available_qty", nullable = false)
    private Integer availableQty;

    @Column(name = "reserved_qty", nullable = false)
    private Integer reservedQty;

    // Optimistic locking — prevents two concurrent orders from overselling
    // the same product. Hibernate auto-increments this on every update and
    // throws OptimisticLockException if two txns clash.
    @Version
    private Long version;
}
