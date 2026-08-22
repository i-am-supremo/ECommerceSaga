package com.saga.inventory.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponse {

    private UUID productId;
    private String sku;
    private String name;
    private BigDecimal price;
    private Integer availableQty;
    private Integer reservedQty;
}
