package fr.guezrou.ecommerce.order.exceptions;

public class OrderCreationException extends RuntimeException {

    public OrderCreationException(String msg) {
        super(msg);
    }
}
