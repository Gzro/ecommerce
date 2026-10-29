package fr.guezrou.ecommerce.order.event.publisher;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import fr.guezrou.ecommerce.order.event.dto.ReleaseStockCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class StockCommandPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishReleaseStock(Long orderId) {

        log.info("Publication ReleaseStockCommand : orderId={}", orderId);

        ReleaseStockCommand command = new ReleaseStockCommand(orderId);

        kafkaTemplate.send("release-stock", orderId.toString(), command);
    }
}
