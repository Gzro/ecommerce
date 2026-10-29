package fr.guezrou.ecommerce.payment.event.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockReservedEvent {

    private Long orderId;
    private Long customerId;
    private BigDecimal totalAmount;
}
