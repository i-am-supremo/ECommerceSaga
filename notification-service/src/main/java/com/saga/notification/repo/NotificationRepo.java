package com.saga.notification.repo;

import com.saga.notification.entity.Notification;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationRepo extends MongoRepository<Notification, String> {
    List<Notification> findByOrderId(UUID orderId);
}
