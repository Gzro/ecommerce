package fr.guezrou.ecommerce.order.exceptions;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(OrderCreationException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientStock(OrderCreationException exception) {

        ErrorResponse response = new ErrorResponse(LocalDateTime.now(), 409, "Order Creation Error",
                exception.getMessage());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }
}