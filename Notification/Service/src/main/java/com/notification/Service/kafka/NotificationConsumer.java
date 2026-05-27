package com.notification.Service.kafka;

import com.notification.Service.Event.OrderCancelledEvent;
import com.notification.Service.Event.OrderPlacedEvent;
import com.notification.Service.Event.UserRegisteredEvent;
import com.notification.Service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Slf4j
@Component
public class NotificationConsumer {
    private final NotificationService notificationService;
    @KafkaListener(
            topics = "users.events",
            groupId = "notification-group",
            containerFactory = "userEventListenerFactory"
    )
    public  void consumerUserEvent(UserRegisteredEvent event){
        log.info("Received UserRegisteredEvent for: {}", event.getEmail());
        notificationService.handleUserRegistered(event);
    }
    @KafkaListener(
            topics = "order.placed",
            groupId = "notification-group",
            containerFactory = "orderPlacedListenerFactory"
    )
    public void consumeOrderPlacedEvent(OrderPlacedEvent event) {
        log.info("Received OrderPlacedEvent for orderId: {}", event.getOrderId());
        notificationService.handleOrderPlaced(event);
    }

    @KafkaListener(
            topics = "order.cancelled",
            groupId = "notification-group",
            containerFactory = "orderCancelledListenerFactory"
    )
    public void consumeOrderCancelledEvent(OrderCancelledEvent event) {
        log.info("Received OrderCancelledEvent for orderId: {}", event.getOrderId());
        notificationService.handleOrderCancelled(event);
    }

}
