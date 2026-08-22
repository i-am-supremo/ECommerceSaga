package com.saga.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {

    @NotNull(message = "customerId is required")
    private UUID customerId;

    @NotEmpty(message = "order must have at least one item")
    @Valid
    private List<OrderItemRequest> items;
}
