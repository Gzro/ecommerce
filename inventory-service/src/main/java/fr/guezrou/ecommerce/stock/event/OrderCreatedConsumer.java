package fr.guezrou.ecommerce.stock.event;

import java.util.List;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import fr.guezrou.ecommerce.stock.config.RabbitMQConfig;
import fr.guezrou.ecommerce.stock.exceptions.InsufficientStockException;
import fr.guezrou.ecommerce.stock.rest.request.ReserveStockRequest;
import fr.guezrou.ecommerce.stock.rest.request.StockReservationItemRequest;
import fr.guezrou.ecommerce.stock.rest.response.ReserveStockResponse;
import fr.guezrou.ecommerce.stock.service.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderCreatedConsumer {

    private final StockService stockService;
    private final StockEventPublisher stockEventPublisher;

    @RabbitListener(queues = RabbitMQConfig.ORDER_CREATED_QUEUE)
    public void consume(OrderCreatedEvent event) {

        log.info("OrderCreated received by StockService: orderId={}", event.getOrderId());

        ReserveStockRequest request = new ReserveStockRequest();

        request.setOrderId(event.getOrderId());

        List<StockReservationItemRequest> items = event.getLines().stream()
                .map(line -> new StockReservationItemRequest(line.getProductId(), line.getQuantity())).toList();

        request.setItems(items);

        try {
            ReserveStockResponse reserveStock = stockService.reserveStock(request);
            if ("RESERVED".equals(reserveStock.getStatus())) {
                log.info("Stock reserved for orderId={}", event.getOrderId());
                StockReservedEvent stockReservedEvent = new StockReservedEvent(event.getOrderId());

                stockEventPublisher.publishStockReserved(stockReservedEvent);
            }
        } catch (InsufficientStockException e) {
            log.warn("Stock reservation failed for orderId={}: {}", event.getOrderId(), e.getMessage());

            stockEventPublisher
                    .publishStockReservationFailed(new StockReservationFailedEvent(event.getOrderId(), e.getMessage()));
        }
    }
}
