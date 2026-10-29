package fr.guezrou.ecommerce.order.event.publisher;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import fr.guezrou.ecommerce.order.event.dto.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventPublisher {

    private static final String ORDER_CREATED_TOPIC = "order-created";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishOrderCreated(OrderCreatedEvent event) {

        kafkaTemplate.send(ORDER_CREATED_TOPIC, event.getOrderId().toString(), event).whenComplete((result, ex) -> {

            if (ex != null) {
                log.error("Erreur lors de l'envoi de OrderCreated : orderId={}", event.getOrderId(), ex);
                return;
            }

            log.info("OrderCreated envoyé : orderId={}, partition={}, offset={}", event.getOrderId(),
                    result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
        });
    }
}
