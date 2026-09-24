package fr.guezrou.ecommerce.order.rest.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fr.guezrou.ecommerce.order.event.OrderCreatedEvent;
import fr.guezrou.ecommerce.order.event.OrderCreatedLineEvent;
import fr.guezrou.ecommerce.order.event.OrderEventPublisher;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/test/rabbit")
@RequiredArgsConstructor
public class RabbitTestController {

    private final OrderEventPublisher orderEventPublisher;

    @PostMapping
    public ResponseEntity<Void> publish() {

        List<OrderCreatedLineEvent> lines = List.of(new OrderCreatedLineEvent(1L, 2, new BigDecimal("1000.00")),
                new OrderCreatedLineEvent(2L, 1, new BigDecimal("400.00")));
        OrderCreatedEvent event = new OrderCreatedEvent(1001L, 123654L, new BigDecimal("2400.00"), lines);

        orderEventPublisher.publishOrderCreated(event);

        return ResponseEntity.ok().build();
    }
}
