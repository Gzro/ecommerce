package fr.guezrou.ecommerce.stock.exceptions;

public class StockNotFoundException extends RuntimeException {

    public StockNotFoundException(Long productId) {
        super("Stock not found for product " + productId);
    }
}
