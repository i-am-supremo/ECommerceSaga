package com.saga.notification.entity;

import com.saga.notification.entity.enums.NotificationType;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.UUID;

@Document(collection = "notifications")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    private String id; // Mongo generates ObjectId string, no need for UUID here

    private UUID orderId;

    private NotificationType type;

    private String message;

    private NotificationStatus status;

    private LocalDateTime createdAt;
}
