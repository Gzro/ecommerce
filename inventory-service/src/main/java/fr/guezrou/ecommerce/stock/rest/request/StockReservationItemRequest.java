package fr.guezrou.ecommerce.stock.rest.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockReservationItemRequest {

    private Long productId;

    private Integer quantity;
}
