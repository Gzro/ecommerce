package fr.guezrou.ecommerce.order.rest.request;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CreateOrderRequest {

    private Long customerId;

    private BigDecimal totalAmount;

    private List<OrderLineRequest> lines = new ArrayList<>();

}
