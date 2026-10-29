package fr.guezrou.ecommerce.order.event.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedEvent {

    private Long orderId;

    private Long customerId;

    private BigDecimal totalAmount;

    private List<OrderLineEvent> lines;
}
