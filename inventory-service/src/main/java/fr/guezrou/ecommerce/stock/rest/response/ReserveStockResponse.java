package fr.guezrou.ecommerce.stock.rest.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ReserveStockResponse {

    private Long orderId;

    private String status;

    private String message;
}
