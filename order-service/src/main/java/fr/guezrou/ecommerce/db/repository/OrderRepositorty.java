package fr.guezrou.ecommerce.db.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepositorty extends JpaRepository<OrderEntity, Long> {

    OrderEntity save(OrderEntity orderEntity);

}