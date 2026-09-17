package fr.guezrou.ecommerce.payment.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fr.guezrou.ecommerce.payment.db.entity.PaymentEntity;
import fr.guezrou.ecommerce.payment.db.entity.PaymentStatus;
import fr.guezrou.ecommerce.payment.db.repository.PaymentRepository;
import fr.guezrou.ecommerce.payment.rest.request.PaymentRequest;
import fr.guezrou.ecommerce.payment.rest.response.PaymentResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;

    @Transactional
    public PaymentResponse processPayment(PaymentRequest request) {

        PaymentEntity payment = new PaymentEntity();

        payment.setOrderId(request.getOrderId());
        payment.setCustomerId(request.getCustomerId());
        payment.setAmount(request.getAmount());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setCreatedAt(LocalDateTime.now());

        paymentRepository.save(payment);

        // Simulation du paiement appel au partenaire
        boolean paymentAccepted = false;

        if (paymentAccepted) {
            payment.setStatus(PaymentStatus.COMPLETED);
        } else {
            payment.setStatus(PaymentStatus.FAILED);
        }
        paymentRepository.save(payment);

        return new PaymentResponse(payment.getId(), payment.getOrderId(), payment.getStatus().name(),
                payment.getAmount());
    }
}