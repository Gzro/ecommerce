package fr.guezrou.ecommerce.order.rest.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StockReservationItemRequest {

    private Long productId;

    private Integer quantity;
}
