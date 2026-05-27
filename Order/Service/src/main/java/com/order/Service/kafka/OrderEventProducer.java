package com.order.Service.kafka;

import com.order.Service.event.OrderCancelledEvent;
import com.order.Service.event.OrderPlacedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderEventProducer {
    private static final String ORDER_PLACED_TOPIC = "order.placed";
    private static final String ORDER_CANCELLED_TOPIC = "order.cancelled";
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishOrderPlaced(OrderPlacedEvent event) {
        kafkaTemplate.send(ORDER_PLACED_TOPIC, event.getOrderId().toString(), event);
        log.info("Published OrderPlacedEvent for orderId: {}", event.getOrderId());
    }

    public void publishOrderCancelled(OrderCancelledEvent event) {
        kafkaTemplate.send(ORDER_CANCELLED_TOPIC, event.getOrderId().toString(), event);
        log.info("Published OrderCancelledEvent for orderId: {}", event.getOrderId());
    }
}
