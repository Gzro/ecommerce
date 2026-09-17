package fr.guezrou.ecommerce.order.rest.request;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReserveStockRequest {

    private Long orderId;

    private List<StockReservationItemRequest> items;
}
