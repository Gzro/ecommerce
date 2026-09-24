package fr.guezrou.ecommerce.stock.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockReservedEvent {

    private Long orderId;
}
