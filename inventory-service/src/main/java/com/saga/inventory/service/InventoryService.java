package com.saga.inventory.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saga.inventory.dto.CreateProductRequest;
import com.saga.inventory.dto.ProductResponse;
import com.saga.inventory.entity.Inventory;
import com.saga.inventory.entity.OutboxEvent;
import com.saga.inventory.entity.Product;
import com.saga.inventory.entity.Reservation;
import com.saga.inventory.entity.enums.ReservationStatus;
import com.saga.inventory.exception.InsufficientStockException;
import com.saga.inventory.exception.ProductNotFoundException;
import com.saga.inventory.kafka.InventoryCommandPayload;
import com.saga.inventory.kafka.InventoryEventPayload;
import com.saga.inventory.kafka.InventoryEventType;
import com.saga.inventory.kafka.Item;
import com.saga.inventory.repo.InventoryRepository;
import com.saga.inventory.repo.OutboxEventRepository;
import com.saga.inventory.repo.ProductRepository;
import com.saga.inventory.repo.ReservationRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ReservationRepository reservationRepository;
    private final ProductRepository productRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void reserveInventory(InventoryCommandPayload commandPayload) {
        try{
            for(Item item : commandPayload.items())
            {
                Inventory inventory = inventoryRepository.findById(item.productId()).orElseThrow(() -> new ProductNotFoundException("Product Not Found: "+ item.productId()));

                if (inventory.getAvailableQty() < item.quantity()) {
                    throw new InsufficientStockException(
                            "Insufficient stock for product " + item.productId()
                                    + " (requested=" + item.quantity() + ", available=" + inventory.getAvailableQty() + ")");
                }

                inventory.setAvailableQty(inventory.getAvailableQty() - item.quantity());
                inventory.setReservedQty(inventory.getReservedQty() + item.quantity());
                inventoryRepository.save(inventory);

                Reservation reservation = Reservation.builder()
                        .orderId(commandPayload.orderId())
                        .sagaId(commandPayload.sagaId())
                        .productId(item.productId())
                        .quantity(item.quantity())
                        .status(ReservationStatus.RESERVED)
                        .build();
                reservationRepository.save(reservation);
            }

            publishEvent(commandPayload.orderId(), commandPayload.sagaId(), InventoryEventType.INVENTORY_RESERVED, null);
            log.info("Inventory reserved for order {}, (sagaId={})", commandPayload.orderId(), commandPayload.sagaId());

        }catch(Exception e){
            publishFailureEvent(commandPayload, e.getMessage());
        }
    }

    private void publishFailureEvent(InventoryCommandPayload command, String reason) {
        publishEvent(command.orderId(), command.sagaId(), InventoryEventType.INVENTORY_RESERVATION_FAILED, reason);
        log.warn("Inventory reservation failed for order {} (sagaId={}): {}", command.orderId(), command.sagaId(), reason);
    }

    private void publishEvent(UUID orderId, UUID sagaId, String eventType, String reason) {
        InventoryEventPayload payload = new InventoryEventPayload(sagaId,orderId,statusFor(eventType), reason, LocalDateTime.now());
        try{
            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateType("INVENTORY")
                    .aggregateId(orderId)
                    .eventType(eventType)
                    .payload(objectMapper.writeValueAsString(payload))
                    .processed(false)
                    .build();
            outboxEventRepository.save(outboxEvent);
        }catch(JsonProcessingException e){
            throw new IllegalStateException("Failed to serialize outbox payload for order "+ orderId, e);
        }

    }

    private String statusFor(String eventType) {
        return switch (eventType)
        {
            case InventoryEventType.INVENTORY_RESERVED -> "RESERVED";
            case InventoryEventType.INVENTORY_RESERVATION_FAILED ->  "FAILED";
            case InventoryEventType.INVENTORY_RELEASED ->  "RELEASED";
            default -> "UNKNOWN";
        };
    }

    @Transactional
    public void releaseInventory(InventoryCommandPayload commandPayload) {
        var reservations = reservationRepository.findByOrderIdAndStatus(commandPayload.orderId(), ReservationStatus.RESERVED);

        for (Reservation reservation : reservations) {
            Inventory inventory = inventoryRepository.findById(reservation.getProductId()).orElseThrow(() -> new ProductNotFoundException("Product Not Found: "+ reservation.getProductId()));
            inventory.setAvailableQty(inventory.getAvailableQty() + reservation.getQuantity());
            inventory.setReservedQty(inventory.getReservedQty() - reservation.getQuantity());
            inventoryRepository.save(inventory);

            reservation.setStatus(ReservationStatus.RELEASED);
            reservationRepository.save(reservation);
        }
        publishEvent(commandPayload.orderId(), commandPayload.sagaId(), InventoryEventType.INVENTORY_RELEASED, null);
        log.info("Inventory released for order {} (sagaId={})", commandPayload.orderId(), commandPayload.sagaId());
    }

    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        Product product = Product.builder()
                .sku(request.getSku())
                .name(request.getName())
                .price(request.getPrice())
                .build();
        product = productRepository.save(product);

        Inventory inventory = Inventory.builder()
                .productId(product.getId())
                .availableQty(request.getInitialStock())
                .reservedQty(0)
                .build();
        inventory = inventoryRepository.save(inventory);

        return toResponse(product, inventory);
    }

    public ProductResponse getProduct(UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found: " + productId));
        Inventory inventory = inventoryRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Inventory not found: " + productId));
        return toResponse(product, inventory);
    }

    private ProductResponse toResponse(Product product, Inventory inventory) {
        return ProductResponse.builder()
                .productId(product.getId())
                .sku(product.getSku())
                .name(product.getName())
                .price(product.getPrice())
                .availableQty(inventory.getAvailableQty())
                .reservedQty(inventory.getReservedQty())
                .build();
    }
}
