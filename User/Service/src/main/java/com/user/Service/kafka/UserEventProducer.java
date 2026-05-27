package com.user.Service.kafka;

import com.user.Service.event.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.kafka.core.KafkaTemplate;
@Service
@Slf4j
@RequiredArgsConstructor
public class UserEventProducer {
    private static final String TOPIC ="users.events";
    private final KafkaTemplate<String, UserRegisteredEvent> kafkaTemplate;
    public void publishUserRegistered(UserRegisteredEvent event){
        kafkaTemplate.send(TOPIC,event.getEmail(),event);
        log.info("Published UserRegisteredEvent for: {}", event.getEmail());
    }

}
