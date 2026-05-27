package com.notification.Service.Controller;

import com.notification.Service.Entity.Notification;
import com.notification.Service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Notification>> getNotification(@PathVariable Long userId){
        return ResponseEntity.ok(notificationService.getNotificationsByUser(userId));
    }
    @GetMapping("/user/{userId}/unread-count")
    public ResponseEntity<Long> getUnreadCount(@PathVariable Long userId) {
        return ResponseEntity.ok(notificationService.getSentCount(userId));
    }

}
