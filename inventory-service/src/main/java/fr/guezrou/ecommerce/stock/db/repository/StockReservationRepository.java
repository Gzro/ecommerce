package fr.guezrou.ecommerce.stock.db.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import fr.guezrou.ecommerce.stock.db.entity.StockReservationEntity;

public interface StockReservationRepository extends JpaRepository<StockReservationEntity, Long> {

    List<StockReservationEntity> findByOrderId(Long orderId);
}
