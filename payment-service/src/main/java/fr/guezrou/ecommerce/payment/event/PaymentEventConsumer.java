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
public class PaymentEventConsumer {

    private final PaymentService paymentService;

    @RabbitListener(queues = RabbitMQConfig.PAYMENT_REQUEST_QUEUE)
    public void consume(PaymentRequest request) {

        log.info("Payment request received: orderId={}, customerId={}, amount={}", request.getOrderId(),
                request.getCustomerId(), request.getAmount());

        paymentService.processPayment(request);
    }
}
