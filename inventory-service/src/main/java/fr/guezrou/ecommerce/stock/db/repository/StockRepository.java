package fr.guezrou.ecommerce.stock.db.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import fr.guezrou.ecommerce.stock.db.entity.StockEntity;

public interface StockRepository extends JpaRepository<StockEntity, Long> {

    Optional<StockEntity> findByProductId(Long productId);
}
