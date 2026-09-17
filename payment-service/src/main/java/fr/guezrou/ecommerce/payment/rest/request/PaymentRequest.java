package fr.guezrou.ecommerce.payment.rest.request;

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