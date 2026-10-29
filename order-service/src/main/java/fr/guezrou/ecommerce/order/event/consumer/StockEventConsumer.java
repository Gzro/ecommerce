package fr.guezrou.ecommerce.order.event.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import fr.guezrou.ecommerce.order.event.config.KafkaConfig;
import fr.guezrou.ecommerce.order.event.dto.StockReservationFailedEvent;
import fr.guezrou.ecommerce.order.event.dto.StockReservedEvent;
import fr.guezrou.ecommerce.order.service.CreateOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class StockEventConsumer {

    private final CreateOrderService orderService;

    @KafkaListener(topics = KafkaConfig.STOCK_RESERVED_TOPIC, groupId = "order-service-group", properties = {
            "spring.json.value.default.type=fr.guezrou.ecommerce.order.event.dto.StockReservedEvent" })
    public void consumeStockReserved(StockReservedEvent event) {

        log.info("StockReserved reçu : orderId={}", event.getOrderId());

        orderService.handleStockReservedEvent(event.getOrderId());
    }

    @KafkaListener(topics = KafkaConfig.STOCK_RESERVATION_FAILED_TOPIC, groupId = "order-service-group", properties = {
            "spring.json.value.default.type=fr.guezrou.ecommerce.order.event.dto.StockReservationFailedEvent" })
    public void consumeStockReservationFailed(StockReservationFailedEvent event) {

        log.info("StockReservationFailed reçu : orderId={}, reason={}", event.getOrderId(), event.getReason());

        orderService.handleStockReservationFailedEvent(event.getOrderId(), event.getReason());
    }
}
