package fr.guezrou.ecommerce.order.exceptions;

public class StockServiceException extends RuntimeException {

    public StockServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
