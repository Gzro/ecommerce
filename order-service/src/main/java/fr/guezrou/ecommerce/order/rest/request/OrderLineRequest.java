package fr.guezrou.ecommerce.order.rest.request;

import java.math.BigDecimal;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class OrderLineRequest {

    private Long productId;
    private String productName;
    private Integer quantity;
    private BigDecimal unitPrice;

}
