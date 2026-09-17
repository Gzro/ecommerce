package fr.guezrou.ecommerce.order.rest.controller;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fr.guezrou.ecommerce.order.rest.request.CreateOrderRequest;
import fr.guezrou.ecommerce.order.rest.response.OrderResponse;
import fr.guezrou.ecommerce.order.service.CreateOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final CreateOrderService orderService;

    @PostMapping("")
    public ResponseEntity<OrderResponse> createOrder(@RequestBody CreateOrderRequest request) {

        log.info("appel au service création de commande, {}", LocalDateTime.now());
        OrderResponse order = orderService.createOrder(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable Long id) {

        // orderService.getOrder(id)
        return null;
    }
}
