package fr.guezrou.ecommerce.order.event.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import fr.guezrou.ecommerce.order.event.config.KafkaConfig;
import fr.guezrou.ecommerce.order.event.dto.PaymentCompletedEvent;
import fr.guezrou.ecommerce.order.event.dto.PaymentFailedEvent;
import fr.guezrou.ecommerce.order.service.CreateOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentCompletedEventConsumer {

    private final CreateOrderService orderService;

    @KafkaListener(topics = KafkaConfig.PAYMENT_COMPLETED_TOPIC, groupId = "payment-service-group", properties = {
            "spring.json.value.default.type=fr.guezrou.ecommerce.order.event.dto.PaymentCompletedEvent" })
    public void consumeStockReserved(PaymentCompletedEvent event) {

        log.info("PaymentCompleted reçu : orderId={}", event.getOrderId());

        orderService.handlePaymentCompletedEvent(event.getOrderId());
    }

    @KafkaListener(topics = KafkaConfig.PAYMENT_FAILED_TOPIC, groupId = "payment-service-group", properties = {
            "spring.json.value.default.type=fr.guezrou.ecommerce.order.event.dto.PaymentFailedEvent" })
    public void consumeStockReservationFailed(PaymentFailedEvent event) {

        log.info("PaymentFailed reçu : orderId={}, reason={}", event.getOrderId(), event.getReason());

        orderService.handlePaymentFailedEvent(event.getOrderId(), event.getReason());
    }
}