package fr.guezrou.ecommerce.order.event.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockReservationFailedEvent {

    private Long orderId;
    private String reason;
}
