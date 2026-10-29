package fr.guezrou.ecommerce.stock.event.consumer;

import java.util.List;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import fr.guezrou.ecommerce.stock.db.entity.StockReservationStatus;
import fr.guezrou.ecommerce.stock.event.dto.OrderCreatedEvent;
import fr.guezrou.ecommerce.stock.event.dto.StockReservationFailedEvent;
import fr.guezrou.ecommerce.stock.event.dto.StockReservedEvent;
import fr.guezrou.ecommerce.stock.event.publisher.StockEventPublisher;
import fr.guezrou.ecommerce.stock.exceptions.InsufficientStockException;
import fr.guezrou.ecommerce.stock.exceptions.StockNotFoundException;
import fr.guezrou.ecommerce.stock.rest.request.ReserveStockRequest;
import fr.guezrou.ecommerce.stock.rest.request.StockReservationItemRequest;
import fr.guezrou.ecommerce.stock.rest.response.ReserveStockResponse;
import fr.guezrou.ecommerce.stock.service.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderCreatedConsumer {

    private final StockService stockService;
    private final StockEventPublisher stockEventPublisher;

    @KafkaListener(topics = "order-created")
    public void consume(OrderCreatedEvent event) {

        log.info("OrderCreated reçu par InventoryService : orderId={}, customerId={}, amount={}", event.getOrderId(),
                event.getCustomerId(), event.getTotalAmount());

        List<StockReservationItemRequest> lstLines = event.getLines().stream()
                .map(line -> new StockReservationItemRequest(line.getProductId(), line.getQuantity())).toList();

        ReserveStockRequest reserveStockRequest = new ReserveStockRequest(event.getOrderId(), lstLines);
        try {
            ReserveStockResponse reservation = stockService.reserveStock(reserveStockRequest);
            if (StockReservationStatus.RESERVED.equals(reservation.getStatus())) {
                stockEventPublisher.publishStockReserved(
                        new StockReservedEvent(event.getOrderId(), event.getCustomerId(), event.getTotalAmount()));
                log.info("Stock réservé et publié avec succès pour la commande {}", event.getOrderId());
            }

        } catch (StockNotFoundException | InsufficientStockException ex) {
            log.warn("Erreur lors de la réservation du stock pour la commande {}: {}", event.getOrderId(),
                    ex.getMessage());
            stockEventPublisher.publishStockReservationFailed(
                    new StockReservationFailedEvent(event.getOrderId(), ex.getMessage()));

        }

    }
}