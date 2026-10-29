package fr.guezrou.ecommerce.order.event.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderLineEvent {

    private Long productId;

    private Integer quantity;
}
