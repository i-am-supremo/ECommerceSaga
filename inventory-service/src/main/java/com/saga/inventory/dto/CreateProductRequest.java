package com.saga.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateProductRequest {
    @NotBlank(message = "sku is required")
    private String sku;

    @NotBlank(message = "name is required")
    private String name;

    @NotNull(message = "price is required")
    private BigDecimal price;

    @NotNull(message = "initialStock is required")
    @Min(value = 0, message = "initialStock cannot be negative")
    private Integer initialStock;
}
