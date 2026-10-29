package fr.guezrou.ecommerce.stock.event.dto;

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
