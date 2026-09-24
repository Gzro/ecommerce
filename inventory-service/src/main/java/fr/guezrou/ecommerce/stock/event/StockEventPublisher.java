package fr.guezrou.ecommerce.stock.event;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import fr.guezrou.ecommerce.stock.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;

/**
 * StockEventPublisher
 */
@Component
@RequiredArgsConstructor
public class StockEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishStockReserved(StockReservedEvent event) {

        rabbitTemplate.convertAndSend(RabbitMQConfig.STOCK_EXCHANGE, RabbitMQConfig.STOCK_RESERVED_ROUTING_KEY, event);
    }

    public void publishStockReservationFailed(StockReservationFailedEvent event) {

        rabbitTemplate.convertAndSend(RabbitMQConfig.STOCK_EXCHANGE,
                RabbitMQConfig.STOCK_RESERVATION_FAILED_ROUTING_KEY, event);
    }

}
