package fr.guezrou.ecommerce.payment.event.consumer;

import org.springframework.stereotype.Component;

import fr.guezrou.ecommerce.payment.event.dto.OrderCreatedEvent;
import fr.guezrou.ecommerce.payment.rest.request.PaymentRequest;
import fr.guezrou.ecommerce.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderCreatedConsumer {

    private final PaymentService paymentService;

    // on ne consomme pas le OrderCreatedEvent venant de order-service, mais
    // StockReservedEvent
    // venant de inventory-service, car c'est lui qui a réservé le stock, la
    // paiement ne doit se faire que si le stock est réservé avec succès
    // @KafkaListener(topics = "order-created", groupId = "payment-service-group")
    public void consume(OrderCreatedEvent event) {

        log.info("OrderCreated reçu par PaymentService : orderId={}, amount={}", event.getOrderId(),
                event.getTotalAmount());

        PaymentRequest request = new PaymentRequest(event.getOrderId(), event.getCustomerId(), event.getTotalAmount());
        paymentService.processPayment(request);
    }
}
