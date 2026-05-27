package com.notification.Service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notification.Service.Entity.Notification;
import com.notification.Service.Entity.NotificationStatus;
import com.notification.Service.Entity.NotificationType;
import com.notification.Service.Event.OrderCancelledEvent;
import com.notification.Service.Event.OrderPlacedEvent;
import com.notification.Service.Event.UserRegisteredEvent;
import com.notification.Service.Repository.NotificationRepository;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



@Slf4j
@RequiredArgsConstructor
@Service
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final EmailService emailService;
    private final ObjectMapper objectMapper;
    public void handleUserRegistered(UserRegisteredEvent event){
        Notification notification=Notification.builder()
                .userId(event.getUserId())
                .email(event.getEmail())
                .notificationType(NotificationType.USER_REGISTERED)
                .status(NotificationStatus.PENDING)
                .payload(toJson(event))
                .build();
        notification=notificationRepository.save(notification);
        try {
            Map<String, Object> model = new HashMap<>();
            model.put("name", event.getName());
            model.put("email", event.getEmail());
            emailService.sendEmail(
                    event.getEmail(),
                    "welcome to kafka project",
                    "user-registered.ftl",
                    model
            );
            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());

        } catch (Exception e) {
            notification.setStatus(NotificationStatus.FAILED);
            notification.setRetryCount(notification.getRetryCount()+1);
            log.error("Failed to send welcome email to: {}", event.getEmail());
        }
        notificationRepository.save(notification);

    }
    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }
    public void handleOrderPlaced(OrderPlacedEvent event){
        Notification notification = Notification.builder()
                .userId(event.getUserId())
                .email("user" + event.getUserId() + "@test.com")
                .notificationType(NotificationType.ORDER_PLACED)
                .status(NotificationStatus.PENDING)
                .payload(toJson(event))
                .build();
        //not necessary this save as we are not using id
        notification=notificationRepository.save(notification);
        try{
            Map<String,Object> model=new HashMap<>();
            model.put("orderId",event.getOrderId());
            model.put("product",event.getProduct());
            emailService.sendEmail(
                    notification.getEmail(),
                    "order placed - #" + event.getOrderId(),
                    "order-placed.ftl",
                    model
            );
            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());
        } catch (Exception e) {
            notification.setStatus(NotificationStatus.FAILED);
            notification.setRetryCount(notification.getRetryCount() + 1);
            log.error("Failed to send cancellation email for orderId: {}", event.getOrderId());
        }
        notificationRepository.save(notification);
    }
    public List<Notification> getNotificationsByUser(Long userId) {
        return notificationRepository.findByUserId(userId);
    }

    public void handleOrderCancelled(OrderCancelledEvent event) {
        Notification notification = Notification.builder()
                .userId(event.getUserId())
                .email("user" + event.getUserId() + "@test.com")
                .notificationType(NotificationType.ORDER_CANCELLED)
                .status(NotificationStatus.PENDING)
                .payload(toJson(event))
                .build();

        notification = notificationRepository.save(notification);

        try {
            Map<String, Object> model = new HashMap<>();
            model.put("orderId", event.getOrderId());
            model.put("product", event.getProduct());

            emailService.sendEmail(
                    notification.getEmail(),
                    "Order Cancelled - #" + event.getOrderId(),
                    "order-cancelled.ftl",
                    model
            );

            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());
            log.info("Cancellation email sent for orderId: {}", event.getOrderId());

        } catch (Exception e) {
            notification.setStatus(NotificationStatus.FAILED);
            notification.setRetryCount(notification.getRetryCount() + 1);
            log.error("Failed to send cancellation email for orderId: {}", event.getOrderId());
        }

        notificationRepository.save(notification);
    }
    public long getSentCount(Long userId) {
        return notificationRepository.countByUserIdAndStatus(
                userId,NotificationStatus.SENT);
    }
}
