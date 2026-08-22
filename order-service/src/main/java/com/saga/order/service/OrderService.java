package com.saga.order.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.saga.order.dto.CreateOrderRequest;
import com.saga.order.exception.OrderNotFoundException;
import com.saga.order.dto.OrderItemRequest;
import com.saga.order.dto.OrderResponse;
import com.saga.order.entity.Order;
import com.saga.order.entity.OrderItem;
import com.saga.order.entity.OutboxEvent;
import com.saga.order.entity.enums.OrderStatus;
import com.saga.order.kafka.OrderEventPayload;
import com.saga.order.kafka.OrderEventType;
import com.saga.order.repo.OrderRepository;
import com.saga.order.repo.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        Order order = Order.builder()
                .customerId(request.getCustomerId())
                .status(OrderStatus.CREATED)
                .totalAmount(calculateTotal(request.getItems()))
                .build();

        for (OrderItemRequest itemRequest : request.getItems()) {
            OrderItem item = OrderItem.builder()
                    .productId(itemRequest.getProductId())
                    .quantity(itemRequest.getQuantity())
                    .unitPrice(itemRequest.getUnitPrice())
                    .build();
            order.addItem(item);
        }

        Order savedOrder = orderRepository.save(order);

        OrderEventPayload payload = new OrderEventPayload(
                savedOrder.getId(),
                null,
                savedOrder.getCustomerId(),
                savedOrder.getTotalAmount(),
                savedOrder.getItems().stream()
                        .map(i -> new OrderEventPayload.Item(i.getProductId(), i.getQuantity(), i.getUnitPrice()))
                        .collect(Collectors.toList()),
                savedOrder.getStatus().name(),
                LocalDateTime.now()
        );

        saveOutboxEvent(savedOrder.getId(), OrderEventType.ORDER_CREATED, payload);

        log.info("Order {} created and OrderCreated outbox event written in same transaction", savedOrder.getId());

        return OrderResponse.from(savedOrder);
    }

    @Transactional
    public void confirmOrder(UUID orderId, UUID sagaId) {
        Order order = getOrderOrThrow(orderId);
        order.setStatus(OrderStatus.CONFIRMED);
        orderRepository.save(order);

        publishStatusChangeEvent(order, sagaId, OrderEventType.ORDER_CONFIRMED);
        log.info("Order {} confirmed (sagaId={})", orderId, sagaId);
    }

    @Transactional
    public void cancelOrder(UUID orderId, UUID sagaId) {
        Order order = getOrderOrThrow(orderId);
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        publishStatusChangeEvent(order, sagaId, OrderEventType.ORDER_CANCELLED);
        log.info("Order {} cancelled / compensated (sagaId={})", orderId, sagaId);
    }

    public OrderResponse getOrder(UUID orderId) {
        return OrderResponse.from(getOrderOrThrow(orderId));
    }

    private Order getOrderOrThrow(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));
    }

    private void publishStatusChangeEvent(Order order, UUID sagaId, String eventType) {
        OrderEventPayload payload = new OrderEventPayload(
                order.getId(),
                sagaId,
                order.getCustomerId(),
                order.getTotalAmount(),
                order.getItems().stream()
                        .map(i -> new OrderEventPayload.Item(i.getProductId(), i.getQuantity(), i.getUnitPrice()))
                        .collect(Collectors.toList()),
                order.getStatus().name(),
                LocalDateTime.now()
        );
        saveOutboxEvent(order.getId(), eventType, payload);
    }

    private void saveOutboxEvent(UUID aggregateId, String eventType, OrderEventPayload payload) {
        try {
            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateType("ORDER")
                    .aggregateId(aggregateId)
                    .eventType(eventType)
                    .payload(objectMapper.writeValueAsString(payload))
                    .processed(false)
                    .build();
            outboxEventRepository.save(outboxEvent);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize outbox payload for aggregate " + aggregateId, e);
        }
    }

    private BigDecimal calculateTotal(List<OrderItemRequest> items) {
        return items.stream()
                .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}