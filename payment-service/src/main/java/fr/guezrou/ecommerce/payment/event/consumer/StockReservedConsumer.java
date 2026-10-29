package fr.guezrou.ecommerce.payment.event.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import fr.guezrou.ecommerce.payment.event.dto.StockReservedEvent;
import fr.guezrou.ecommerce.payment.rest.request.PaymentRequest;
import fr.guezrou.ecommerce.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class StockReservedConsumer {

    private final PaymentService paymentService;

    @KafkaListener(topics = "stock-reserved", groupId = "payment-service-group")
    public void consume(StockReservedEvent event) {

        log.info("StockReserved reçu par PaymentService : orderId={}", event.getOrderId());

        PaymentRequest request = new PaymentRequest(event.getOrderId(), event.getCustomerId(), event.getTotalAmount());

        paymentService.processPayment(request);
    }
}
