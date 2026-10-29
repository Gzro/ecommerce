package fr.guezrou.ecommerce.stock.event.publisher;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import fr.guezrou.ecommerce.stock.event.config.KafkaConfig;
import fr.guezrou.ecommerce.stock.event.dto.StockReservationFailedEvent;
import fr.guezrou.ecommerce.stock.event.dto.StockReservedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class StockEventPublisher {

    private final KafkaTemplate<String, StockReservedEvent> stockReservedKafkaTemplate;
    private final KafkaTemplate<String, StockReservationFailedEvent> stockFailedKafkaTemplate;

    public void publishStockReserved(StockReservedEvent event) {

        stockReservedKafkaTemplate.send(KafkaConfig.STOCK_RESERVED_TOPIC, event.getOrderId().toString(), event);
    }

    public void publishStockReservationFailed(StockReservationFailedEvent event) {

        stockFailedKafkaTemplate.send(KafkaConfig.STOCK_RESERVATION_FAILED_TOPIC, event.getOrderId().toString(), event);
    }
}
