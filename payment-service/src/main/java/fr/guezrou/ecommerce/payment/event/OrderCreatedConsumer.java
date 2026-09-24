package fr.guezrou.ecommerce.payment.event;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import fr.guezrou.ecommerce.payment.config.RabbitMQConfig;
import fr.guezrou.ecommerce.payment.rest.request.PaymentRequest;
import fr.guezrou.ecommerce.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderCreatedConsumer {

    private final PaymentService paymentService;

    @RabbitListener(queues = RabbitMQConfig.ORDER_CREATED_QUEUE)
    public void consume(OrderCreatedEvent event) {

        log.info("OrderCreated received by PaymentService: orderId={}, amount={}", event.getOrderId(),
                event.getTotalAmount());

        paymentService
                .processPayment(new PaymentRequest(event.getOrderId(), event.getCustomerId(), event.getTotalAmount()));
    }
}
