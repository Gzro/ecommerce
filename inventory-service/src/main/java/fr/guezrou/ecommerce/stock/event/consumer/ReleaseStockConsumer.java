package fr.guezrou.ecommerce.stock.event.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import fr.guezrou.ecommerce.stock.event.dto.ReleaseStockCommand;
import fr.guezrou.ecommerce.stock.service.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReleaseStockConsumer {

    private final StockService stockService;

    @KafkaListener(topics = "release-stock", groupId = "inventory-service-group", properties = {
            "spring.json.value.default.type=fr.guezrou.ecommerce.stock.event.dto.ReleaseStockCommand" })
    public void consume(ReleaseStockCommand command) {

        log.info("ReleaseStock reçu par InventoryService : orderId={}", command.getOrderId());

        stockService.releaseStock(command.getOrderId());
    }
}
