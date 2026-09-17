package fr.guezrou.ecommerce.stock.rest.request;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ReserveStockRequest {

    private Long orderId;

    private List<StockReservationItemRequest> items = new ArrayList<>();
}
