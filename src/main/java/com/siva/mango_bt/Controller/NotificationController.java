package com.siva.mango_bt.Controller;

import com.siva.mango_bt.Repos.OrderRepository;
import com.siva.mango_bt.Service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private OrderRepository orderRepository;

    // ADMIN: Subscribe to real-time order notifications
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamNotifications() {
        return notificationService.createEmitter();
    }

    // ADMIN: Get count of new unread orders
    @GetMapping("/unread-count")
    public ResponseEntity<?> getUnreadCount() {
        long count = orderRepository.countByIsNewNotificationTrue();
        return ResponseEntity.ok(Map.of("count", count));
    }

    // ADMIN: Mark all notifications as read
    @PostMapping("/mark-read")
    public ResponseEntity<?> markAllRead() {
        orderRepository.markAllNotificationsRead();
        return ResponseEntity.ok(Map.of("message", "All marked as read"));
    }
}
