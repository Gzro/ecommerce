package fr.guezrou.ecommerce.payment.rest.response;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentResponse {

    private Long paymentId;

    private Long orderId;

    private String status;

    private BigDecimal amount;
}
