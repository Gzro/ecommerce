package fr.guezrou.ecommerce.stock.exceptions;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(StockNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleStockNotFound(StockNotFoundException exception) {

        ErrorResponse response = new ErrorResponse(LocalDateTime.now(), 404, "Stock Not Found", exception.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientStock(InsufficientStockException exception) {

        ErrorResponse response = new ErrorResponse(LocalDateTime.now(), 409, "Insufficient Stock",
                exception.getMessage());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }
}