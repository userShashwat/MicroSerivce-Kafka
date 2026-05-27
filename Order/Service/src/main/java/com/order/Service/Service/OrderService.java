package com.order.Service.Service;

import com.order.Service.DTO.OrderRequest;
import com.order.Service.Entity.Order;
import com.order.Service.Entity.OrderStatus;
import com.order.Service.Repository.OrderRepository;
import com.order.Service.event.OrderCancelledEvent;
import com.order.Service.event.OrderPlacedEvent;
import com.order.Service.kafka.OrderEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderEventProducer orderEventProducer;
    public Order placeOrder(OrderRequest orderRequest){
        Order order=Order.builder()
                .userId(orderRequest.getUserId())
                .product(orderRequest.getProduct())
                .quantity(orderRequest.getQuantity())
                .price(orderRequest.getPrice())
                .status(OrderStatus.PLACED)
                .build();
        Order saved=orderRepository.save(order);
        log.info("Order placed: {}", saved.getId());
        orderEventProducer.publishOrderPlaced(new OrderPlacedEvent(
                saved.getId(),
                saved.getUserId(),
                saved.getProduct(),
                saved.getQuantity(),
                saved.getPrice()
        ));
        return saved;

    }
    public Order orderCancelled(Long orderId){
        Order order=orderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        if(order.getStatus()==OrderStatus.CANCELLED){
            throw new RuntimeException("orderalready cancelled"+orderId);
        }
        order.setStatus(OrderStatus.CANCELLED);
        Order saved=orderRepository.save(order);
        log.info("Order cancelled: {}", saved.getId());
        orderEventProducer.publishOrderCancelled(new OrderCancelledEvent(
                saved.getId(),
                saved.getUserId(),
                saved.getProduct()
        ));
        return saved;
    }
    public List<Order> getOrdersByUser(Long userId) {
        return orderRepository.findByUserId(userId);
    }
}
