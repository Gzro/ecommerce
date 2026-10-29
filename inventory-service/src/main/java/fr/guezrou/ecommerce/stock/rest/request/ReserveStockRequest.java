package fr.guezrou.ecommerce.stock.rest.request;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReserveStockRequest {

    private Long orderId;

    private List<StockReservationItemRequest> items = new ArrayList<>();
}
