package fr.guezrou.ecommerce.order.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fr.guezrou.ecommerce.db.repository.OrderEntity;
import fr.guezrou.ecommerce.db.repository.OrderLineEntity;
import fr.guezrou.ecommerce.db.repository.OrderRepositorty;
import fr.guezrou.ecommerce.order.exceptions.OrderCreationException;
import fr.guezrou.ecommerce.order.exceptions.StockServiceException;
import fr.guezrou.ecommerce.order.rest.client.PaymentClient;
import fr.guezrou.ecommerce.order.rest.client.StockClient;
import fr.guezrou.ecommerce.order.rest.request.CreateOrderRequest;
import fr.guezrou.ecommerce.order.rest.request.OrderLineRequest;
import fr.guezrou.ecommerce.order.rest.request.PaymentRequest;
import fr.guezrou.ecommerce.order.rest.request.ReserveStockRequest;
import fr.guezrou.ecommerce.order.rest.request.StockReservationItemRequest;
import fr.guezrou.ecommerce.order.rest.response.OrderResponse;
import fr.guezrou.ecommerce.order.rest.response.PaymentResponse;
import fr.guezrou.ecommerce.order.rest.response.ReserveStockResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class CreateOrderService {

    private final OrderRepositorty orderRepository;
    private final StockClient stockClient;
    private final PaymentClient paymentClient;

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

        ReserveStockRequest stockRequest = new ReserveStockRequest(savedOrder.getId(), orderRequest.getLines().stream()
                .map(line -> new StockReservationItemRequest(line.getProductId(), line.getQuantity())).toList()

        );

        try {
            ReserveStockResponse stockResponse = stockClient.reserveStock(stockRequest);
            if (!"RESERVED".equals(stockResponse.getStatus())) {
                throw new OrderCreationException("Unable to create order: stock reservation failed");
            }

            PaymentRequest payRequest = new PaymentRequest();
            payRequest.setCustomerId(orderRequest.getCustomerId());
            payRequest.setAmount(orderRequest.getTotalAmount());
            payRequest.setOrderId(savedOrder.getId());
            PaymentResponse payResponse = paymentClient.pay(payRequest);

            if (!"COMPLETED".equals(payResponse.getStatus())) {
                // Compensation
                // payClient.releaseReservation(savedOrder.getId());
                savedOrder.setStatus("CANCELLED");
                orderRepository.save(savedOrder);
                throw new OrderCreationException("Unable to create order: payment failed");
            }

            savedOrder.setStatus("CONFIRMED");
            orderRepository.save(savedOrder);

        } catch (StockServiceException exception) {
            log.warn(exception.getMessage());
            // savedOrder.setStatus("CANCELLED");
            throw new OrderCreationException(String.format("Unable to create order: %s", exception.getMessage()));
        }

        /*
         * 
         * OrderCreatedEvent event = new OrderCreatedEvent(savedOrder.getId(),
         * savedOrder.getCustomerId(), savedOrder.getTotalAmount());
         * 
         * OutboxEventEntity outbox = new OutboxEventEntity();
         * outbox.setEventType("OrderCreated"); outbox.setAggregateType("Order");
         * outbox.setAggregateId(savedOrder.getId().toString());
         * outbox.setPayload(objectMapper.writeValueAsString(event));
         * outbox.setCreatedAt(LocalDateTime.now()); outbox.setPublished(false);
         * 
         * outboxRepository.save(outbox);
         * 
         */

        OrderResponse orderResponse = new OrderResponse();
        orderResponse.setId(savedOrder.getId());
        return orderResponse;
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
