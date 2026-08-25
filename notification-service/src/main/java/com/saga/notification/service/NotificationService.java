package com.saga.notification.service;

import com.saga.notification.entity.Notification;
import com.saga.notification.entity.enums.NotificationStatus;
import com.saga.notification.entity.enums.NotificationType;
import com.saga.notification.kafka.NotificationCommandPayload;
import com.saga.notification.repo.NotificationRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepo notificationRepository;

    /**
     * Mock "send" - no real email/SMS provider here since this is a learning project.
     * We just log it and persist a record, same way Payment mocks a gateway.
     * No outbox event published back - Notification is a leaf node in the saga.
     */
    public void sendNotification(NotificationCommandPayload command) {
        Notification notification = Notification.builder()
                .sagaId(command.sagaId())
                .orderId(command.orderId())
                .type(NotificationType.EMAIL)
                .message(command.message())
                .status(NotificationStatus.SENT) // always succeeds - mock only
                .createdAt(LocalDateTime.now())
                .build();

        notificationRepository.save(notification);

        log.info("[MOCK EMAIL SENT] To customer for order {} -> \"{}\"", command.orderId(), command.message());
    }
}