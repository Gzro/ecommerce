package fr.guezrou.ecommerce.db.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long customerId;

    private BigDecimal totalAmount;

    private String status;

    private LocalDateTime createdAt;

    private boolean stockReserved = false;

    private boolean paymentCompleted = false;

    @Column(name = "cancel_reason")
    private String cancelReason;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderLineEntity> lines = new ArrayList<>();

    public void addLine(OrderLineEntity line) {
        lines.add(line);
        line.setOrder(this);
    }

    public void removeLine(OrderLineEntity line) {
        lines.remove(line);
        line.setOrder(null);
    }

    public void addAllLines(List<OrderLineEntity> lines) {
        lines.clear();
        lines.addAll(lines);
    }

}
