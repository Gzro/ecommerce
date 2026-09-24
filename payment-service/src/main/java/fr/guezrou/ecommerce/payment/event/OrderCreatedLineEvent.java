package fr.guezrou.ecommerce.payment.event;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedLineEvent {

    private Long productId;
    private Integer quantity;
    private BigDecimal unitPrice;
}
