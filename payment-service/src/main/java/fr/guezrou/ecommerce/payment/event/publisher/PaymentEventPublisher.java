package fr.guezrou.ecommerce.payment.event.publisher;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import fr.guezrou.ecommerce.payment.event.config.KafkaConfig;
import fr.guezrou.ecommerce.payment.event.dto.PaymentCompletedEvent;
import fr.guezrou.ecommerce.payment.event.dto.PaymentFailedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventPublisher {

    private final KafkaTemplate<String, PaymentCompletedEvent> paymentCompletedKafkaTemplate;

    private final KafkaTemplate<String, PaymentFailedEvent> paymentFailedKafkaTemplate;

    public void publishPaymentCompleted(PaymentCompletedEvent event) {

        paymentCompletedKafkaTemplate.send(KafkaConfig.PAYMENT_COMPLETED_TOPIC, event.getOrderId().toString(), event)
                .whenComplete((result, ex) -> {

                    if (ex != null) {
                        log.error("Erreur publication PaymentCompleted : orderId={}", event.getOrderId(), ex);
                        return;
                    }

                    log.info("PaymentCompleted publié : orderId={}, partition={}, offset={}", event.getOrderId(),
                            result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
                });
    }

    public void publishPaymentFailed(PaymentFailedEvent event) {
        log.info("PaymentFailed publié : orderId={}", event.getOrderId());
        paymentFailedKafkaTemplate.send(KafkaConfig.PAYMENT_FAILED_TOPIC, event.getOrderId().toString(), event);
    }
}
