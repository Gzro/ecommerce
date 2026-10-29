package fr.guezrou.ecommerce.payment.db.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import fr.guezrou.ecommerce.payment.db.entity.PaymentEntity;

public interface PaymentRepository extends JpaRepository<PaymentEntity, Long> {

    Optional<PaymentEntity> findByOrderId(Long orderId);

}
