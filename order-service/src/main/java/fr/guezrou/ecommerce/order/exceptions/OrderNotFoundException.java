package fr.guezrou.ecommerce.order.exceptions;

public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(Long orderId) {
        super(String.format("Order %s not found ", orderId));
    }
}