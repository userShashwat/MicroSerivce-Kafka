package com.notification.Service.kafka;


import com.notification.Service.Event.OrderCancelledEvent;
import com.notification.Service.Event.OrderPlacedEvent;
import com.notification.Service.Event.UserRegisteredEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Fallback;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
public class KafkaConsumerConfig {
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootStrapServers;
    private Map<String,Object> baseConsumerConfig() {
        Map<String, Object> base = new HashMap<>();
        base.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootStrapServers);
        base.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return base;
    }
    @Bean
    public ConsumerFactory<String, UserRegisteredEvent> userEventConsumerFactory() {
        JsonDeserializer<UserRegisteredEvent> deserializer =
                new JsonDeserializer<>(UserRegisteredEvent.class, false);
        Map<String,Object> user=baseConsumerConfig();
        user.put(ConsumerConfig.GROUP_ID_CONFIG,"notification-group");
        return new DefaultKafkaConsumerFactory<>(user,
                new StringDeserializer(), deserializer);
    }
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String,UserRegisteredEvent>  userEventListenerFactory(){
        ConcurrentKafkaListenerContainerFactory<String, UserRegisteredEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(userEventConsumerFactory());
        return factory;
    }
    @Bean
    public ConsumerFactory<String, OrderPlacedEvent> orderPlacedConsumerFactory(){
        JsonDeserializer<OrderPlacedEvent> deserializer=new JsonDeserializer<>(OrderPlacedEvent.class,false);
        Map<String,Object> mp=baseConsumerConfig();
        mp.put(ConsumerConfig.GROUP_ID_CONFIG,"notification-group");
        return new DefaultKafkaConsumerFactory<>(mp,new StringDeserializer(),deserializer);
    }
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String,OrderPlacedEvent> orderPlacedListenerFactory(){
        ConcurrentKafkaListenerContainerFactory<String,OrderPlacedEvent> factory=new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(orderPlacedConsumerFactory());
        return factory;
    }
    @Bean
    public ConsumerFactory<String, OrderCancelledEvent> orderCancelledConsumerFactory() {
        JsonDeserializer<OrderCancelledEvent> deserializer =
                new JsonDeserializer<>(OrderCancelledEvent.class, false);
        Map<String, Object> props = baseConsumerConfig();
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "notification-group");
        return new DefaultKafkaConsumerFactory<>(props,
                new StringDeserializer(), deserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, OrderCancelledEvent> orderCancelledListenerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, OrderCancelledEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(orderCancelledConsumerFactory());
        return factory;
    }

}
