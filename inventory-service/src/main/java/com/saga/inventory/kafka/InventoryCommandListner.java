package com.saga.inventory.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saga.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryCommandListner {

    private final InventoryService inventoryService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = KafkaTopics.INVENTORY_COMMANDS)
    public void handleCommand(String message) {
        try{
            InventoryCommandPayload commandPayload = objectMapper.readValue(message, InventoryCommandPayload.class);
            log.info("Received inventory command {} for order {} (sagaId={})", commandPayload.commandType(), commandPayload.orderId(), commandPayload.sagaId());

            switch(commandPayload.commandType()){
                case InventoryEventType.RESERVE_INVENTORY ->
                    inventoryService.reserveInventory(commandPayload);
                case InventoryEventType.RELEASE_INVENTORY ->
                    inventoryService.releaseInventory(commandPayload);
                default -> log.warn("Invalid command received {}", commandPayload.commandType());
            }
        } catch (Exception e) {
            log.error("Error while processing command {}", message, e);
        }

    }

}
