package fr.guezrou.ecommerce.order.rest.request;

import java.math.BigDecimal;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PaymentRequest {

    private Long orderId;

    private Long customerId;

    private BigDecimal amount;
}