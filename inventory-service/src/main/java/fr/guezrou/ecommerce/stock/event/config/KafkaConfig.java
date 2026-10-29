package fr.guezrou.ecommerce.stock.event.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    public static final String ORDER_CREATED_TOPIC = "order-created";
    public static final String STOCK_RESERVED_TOPIC = "stock-reserved";
    public static final String STOCK_RESERVATION_FAILED_TOPIC = "stock-reservation-failed";

    @Bean
    public NewTopic orderCreatedTopic() {
        return TopicBuilder.name(ORDER_CREATED_TOPIC).partitions(2).replicas(1).build();
    }

    @Bean
    public NewTopic stockReservedTopic() {
        return TopicBuilder.name(STOCK_RESERVED_TOPIC).partitions(2).replicas(1).build();
    }

    @Bean
    public NewTopic stockReservationFailedTopic() {
        return TopicBuilder.name(STOCK_RESERVATION_FAILED_TOPIC).partitions(2).replicas(1).build();
    }
}
