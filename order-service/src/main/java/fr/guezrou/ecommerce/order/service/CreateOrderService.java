package fr.guezrou.ecommerce.order.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fr.guezrou.ecommerce.db.repository.OrderEntity;
import fr.guezrou.ecommerce.db.repository.OrderLineEntity;
import fr.guezrou.ecommerce.db.repository.OrderRepositorty;
import fr.guezrou.ecommerce.order.event.OrderCreatedEvent;
import fr.guezrou.ecommerce.order.event.OrderCreatedLineEvent;
import fr.guezrou.ecommerce.order.event.OrderEventPublisher;
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
    private final OrderEventPublisher orderEventPublisher;

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

        OrderCreatedEvent eventOrderCreated = new OrderCreatedEvent(savedOrder.getId(), savedOrder.getCustomerId(),
                savedOrder.getTotalAmount(),
                savedOrder.getLines().stream().map(
                        line -> new OrderCreatedLineEvent(line.getProductId(), line.getQuantity(), line.getUnitPrice()))
                        .toList());

        orderEventPublisher.publishOrderCreated(eventOrderCreated);

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

    public OrderEntity getorderById(Long orderId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getorderById'");
    }
}
