package com.saga.notification.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saga.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationCommandListener {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = KafkaTopics.NOTIFICATION_COMMANDS, groupId = "notification-service")
    public void handleCommand(String message) {
        try {
            NotificationCommandPayload command = objectMapper.readValue(message, NotificationCommandPayload.class);
            log.info("Received command {} for order {} (sagaId={})",
                    command.commandType(), command.orderId(), command.sagaId());

            if (NotificationCommandType.SEND_NOTIFICATION.equals(command.commandType())) {
                notificationService.sendNotification(command);
            } else {
                log.warn("Unknown command type received: {}", command.commandType());
            }
        } catch (Exception e) {
            // Notification is non-critical (fire-and-forget) - saga doesn't wait on this,
            // so we just log and move on. No DLT needed for a learning project here.
            log.error("Failed to process notification command message: {}", message, e);
        }
    }
}
