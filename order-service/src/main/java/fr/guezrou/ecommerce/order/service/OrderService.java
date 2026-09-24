package fr.guezrou.ecommerce.order.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fr.guezrou.ecommerce.db.repository.OrderEntity;
import fr.guezrou.ecommerce.db.repository.OrderRepositorty;
import fr.guezrou.ecommerce.order.exceptions.OrderNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepositorty orderRepository;

    public void handleStockReservedEvent(Long orderId) {

        OrderEntity order = orderRepository.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));

        order.setStockReserved(true);

        if (order.isPaymentCompleted()) {
            order.setStatus("CONFIRMED");
        }
        orderRepository.save(order);
    }

    public void handleStockReservationFailed(Long orderId, String reason) {
        OrderEntity order = orderRepository.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));

        order.setStatus("CANCELLED");
        order.setCancelReason(reason);
        orderRepository.save(order);
    }

    @Transactional
    public void handlePaymentCompletedEvent(Long orderId) {

        OrderEntity order = orderRepository.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));

        order.setPaymentCompleted(true);

        if (order.isStockReserved()) {
            order.setStatus("CONFIRMED");
        }

        orderRepository.save(order);
    }

    @Transactional
    public void handlePaymentFailedEvent(Long orderId, String reason) {

        OrderEntity order = orderRepository.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));

        order.setStatus("CANCELLED");
        order.setCancelReason(reason);

        orderRepository.save(order);
    }

}
