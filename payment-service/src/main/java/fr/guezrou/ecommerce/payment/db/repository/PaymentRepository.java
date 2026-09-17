package fr.guezrou.ecommerce.payment.db.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import fr.guezrou.ecommerce.payment.db.entity.PaymentEntity;

public interface PaymentRepository extends JpaRepository<PaymentEntity, Long> {

    PaymentEntity save(PaymentEntity paymentEntity);

}
