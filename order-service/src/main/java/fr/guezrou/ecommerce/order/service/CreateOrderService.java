package fr.guezrou.ecommerce.order.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import fr.guezrou.ecommerce.db.repository.OrderEntity;
import fr.guezrou.ecommerce.db.repository.OrderLineEntity;
import fr.guezrou.ecommerce.db.repository.OrderRepositorty;
import fr.guezrou.ecommerce.db.repository.OutboxEventEntity;
import fr.guezrou.ecommerce.db.repository.OutboxEventRepository;
import fr.guezrou.ecommerce.order.event.config.KafkaConfig;
import fr.guezrou.ecommerce.order.event.dto.OrderCreatedEvent;
import fr.guezrou.ecommerce.order.event.dto.OrderLineEvent;
import fr.guezrou.ecommerce.order.event.publisher.OrderEventPublisher;
import fr.guezrou.ecommerce.order.event.publisher.StockCommandPublisher;
import fr.guezrou.ecommerce.order.exceptions.OrderNotFoundException;
import fr.guezrou.ecommerce.order.rest.request.CreateOrderRequest;
import fr.guezrou.ecommerce.order.rest.request.OrderLineRequest;
import fr.guezrou.ecommerce.order.rest.response.OrderResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class CreateOrderService {

    private final OrderRepositorty orderRepository;

    private final OutboxEventRepository outboxEventRepository;

    public final OrderEventPublisher orderEventPublisher;

    private final StockCommandPublisher stockCommandPublisher;

    private final ObjectMapper objectMapper;

    public OrderResponse createOrder(CreateOrderRequest orderRequest) {

        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setCustomerId(orderRequest.getCustomerId());
        orderEntity.setStatus("PENDING");
        orderEntity.setTotalAmount(orderRequest.getTotalAmount());
        orderEntity.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));

        for (OrderLineRequest lineReq : orderRequest.getLines()) {
            orderEntity.addLine(toOrderLineEntity(lineReq));
        }
        OrderEntity savedOrder = orderRepository.save(orderEntity);

        List<OrderLineEvent> lines = orderRequest.getLines().stream()
                .map(line -> new OrderLineEvent(line.getProductId(), line.getQuantity())).toList();

        OrderCreatedEvent event = new OrderCreatedEvent(savedOrder.getId(), savedOrder.getCustomerId(),
                savedOrder.getTotalAmount(), lines);

        OutboxEventEntity outbox = new OutboxEventEntity();

        outbox.setAggregateType("Order");
        outbox.setAggregateId(savedOrder.getId().toString());
        outbox.setEventType("OrderCreated");
        outbox.setTopic(KafkaConfig.ORDER_CREATED_TOPIC);
        outbox.setMessageKey(savedOrder.getId().toString());

        try {
            outbox.setPayload(objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Impossible de sérialiser OrderCreated", exception);
        }

        outbox.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        outbox.setPublished(false);

        outboxEventRepository.save(outbox);

        OrderResponse orderResponse = new OrderResponse();
        orderResponse.setId(savedOrder.getId());
        return orderResponse;
    }

    @Transactional
    public void handleStockReservedEvent(Long orderId) {

        OrderEntity order = orderRepository.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));

        order.setStockReserved(true);

        if (order.isPaymentCompleted()) {
            order.setStatus("CONFIRMED");
            log.info("Commande {} confirmée ", orderId);
        }

        orderRepository.save(order);
        log.info("Commande {} : stock réservé", orderId);
    }

    @Transactional
    public void handleStockReservationFailedEvent(Long orderId, String reason) {

        OrderEntity order = orderRepository.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));

        order.setStatus("CANCELLED");
        order.setCancelReason(reason);

        orderRepository.save(order);

        log.info("Commande {} annulée : {}", orderId, reason);
    }

    @Transactional
    public void handlePaymentCompletedEvent(Long orderId) {

        OrderEntity order = orderRepository.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));

        order.setPaymentCompleted(true);

        if (order.isStockReserved()) {
            order.setStatus("CONFIRMED");
            log.info("Commande {} confirmée ", orderId);
        }

        orderRepository.save(order);
        log.info("Commande {} : Paiement confirmé", orderId);
    }

    @Transactional
    public void handlePaymentFailedEvent(Long orderId, String reason) {

        OrderEntity order = orderRepository.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
        order.setStatus("CANCELLED");
        order.setCancelReason(reason);
        orderRepository.save(order);
        if (order.isStockReserved()) {
            stockCommandPublisher.publishReleaseStock(order.getId());
        }

        log.warn("Commande {} annulée suite à un échec du paiement", order.getId());
    }

    private OrderLineEntity toOrderLineEntity(OrderLineRequest lineReq) {
        OrderLineEntity entity = new OrderLineEntity();
        entity.setProductId(lineReq.getProductId());
        entity.setProductName(lineReq.getProductName());
        entity.setQuantity(lineReq.getQuantity());
        entity.setUnitPrice(lineReq.getUnitPrice());
        return entity;
    }
}
