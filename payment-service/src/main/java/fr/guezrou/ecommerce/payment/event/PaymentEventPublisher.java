package fr.guezrou.ecommerce.payment.event;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import fr.guezrou.ecommerce.payment.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PaymentEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishPaymentCompleted(PaymentCompletedEvent event) {

        rabbitTemplate.convertAndSend(RabbitMQConfig.PAYMENT_EXCHANGE, RabbitMQConfig.PAYMENT_COMPLETED_ROUTING_KEY,
                event);
    }

    public void publishPaymentFailed(PaymentFailedEvent event) {

        rabbitTemplate.convertAndSend(RabbitMQConfig.PAYMENT_EXCHANGE, RabbitMQConfig.PAYMENT_FAILED_ROUTING_KEY,
                event);
    }
}
