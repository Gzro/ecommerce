package fr.guezrou.ecommerce.db.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, Long> {

    List<OutboxEventEntity> findTop100ByPublishedFalseOrderByIdAsc();
}