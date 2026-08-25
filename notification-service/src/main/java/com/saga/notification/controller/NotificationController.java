package com.saga.notification.controller;

import com.saga.notification.entity.Notification;
import com.saga.notification.repo.NotificationRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepo notificationRepository;

    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<Notification>> getByOrderId(@PathVariable UUID orderId) {
        return ResponseEntity.ok(notificationRepository.findByOrderId(orderId));
    }
}