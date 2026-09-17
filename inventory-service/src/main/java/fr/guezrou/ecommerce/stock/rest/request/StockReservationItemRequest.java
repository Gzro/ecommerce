package fr.guezrou.ecommerce.stock.rest.request;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class StockReservationItemRequest {

    private Long productId;

    private Integer quantity;
}
