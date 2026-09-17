package fr.guezrou.ecommerce.order.rest.response;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ReserveStockResponse {

    private Long orderId;

    private String status;

    private String message;
}
