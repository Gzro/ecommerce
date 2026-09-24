package fr.guezrou.ecommerce.order.event;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import fr.guezrou.ecommerce.order.config.RabbitMQConfig;
import fr.guezrou.ecommerce.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final OrderService orderService;

    @RabbitListener(queues = "order.queue")
    public void consume(OrderCreatedEvent event) {

        log.info("OrderCreated event received: orderId={}, customerId={}, amount={}", event.getOrderId(),
                event.getCustomerId(), event.getTotalAmount());
    }

    @RabbitListener(queues = RabbitMQConfig.STOCK_RESERVED_QUEUE)
    public void consume(StockReservedEvent event) {

        log.info("StockReserved received: orderId={}", event.getOrderId());
        orderService.handleStockReservedEvent(event.getOrderId());

    }

    @RabbitListener(queues = RabbitMQConfig.STOCK_RESERVATION_FAILED_QUEUE)
    public void consume(StockReservationFailedEvent event) {

        log.info("StockReservationFailed received: orderId={}, reason={}", event.getOrderId(), event.getReason());

        orderService.handleStockReservationFailed(event.getOrderId(), event.getReason());
    }

    @RabbitListener(queues = RabbitMQConfig.PAYMENT_COMPLETED_QUEUE)
    public void consume(PaymentCompletedEvent event) {

        log.info("PaymentCompleted received: orderId={}, paymentId={}", event.getOrderId(), event.getPaymentId());

        orderService.handlePaymentCompletedEvent(event.getOrderId());
    }

    @RabbitListener(queues = RabbitMQConfig.PAYMENT_FAILED_QUEUE)
    public void consume(PaymentFailedEvent event) {

        log.info("PaymentFailed received: orderId={}, reason={}", event.getOrderId(), event.getReason());

        orderService.handlePaymentFailedEvent(event.getOrderId(), event.getReason());
    }

}
