package fr.guezrou.ecommerce.payment.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fr.guezrou.ecommerce.payment.db.entity.PaymentEntity;
import fr.guezrou.ecommerce.payment.db.entity.PaymentStatus;
import fr.guezrou.ecommerce.payment.db.repository.PaymentRepository;
import fr.guezrou.ecommerce.payment.event.dto.PaymentCompletedEvent;
import fr.guezrou.ecommerce.payment.event.dto.PaymentFailedEvent;
import fr.guezrou.ecommerce.payment.event.publisher.PaymentEventPublisher;
import fr.guezrou.ecommerce.payment.rest.request.PaymentRequest;
import fr.guezrou.ecommerce.payment.rest.response.PaymentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentEventPublisher paymentEventPublisher;

    @Transactional
    public PaymentResponse processPayment(PaymentRequest request) {

        Optional<PaymentEntity> existingPayment = paymentRepository.findByOrderId(request.getOrderId());

        if (existingPayment.isPresent()) {
            PaymentEntity payment = existingPayment.get();

            log.info("Paiement déjà enregistré : orderId={}, paymentId={}, status={}", payment.getOrderId(),
                    payment.getId(), payment.getStatus());

            return new PaymentResponse(payment.getId(), payment.getOrderId(), payment.getStatus().name(),
                    payment.getAmount());
        }

        PaymentEntity payment = new PaymentEntity();

        payment.setOrderId(request.getOrderId());
        payment.setCustomerId(request.getCustomerId());
        payment.setAmount(request.getAmount());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setCreatedAt(LocalDateTime.now());

        paymentRepository.save(payment);

        // Simulation du paiement appel au partenaire
        boolean paymentAccepted = true;

        if (paymentAccepted) {
            payment.setStatus(PaymentStatus.COMPLETED);
            paymentRepository.save(payment);
            paymentEventPublisher.publishPaymentCompleted(
                    new PaymentCompletedEvent(payment.getOrderId(), payment.getId(), payment.getAmount()));
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            paymentEventPublisher
                    .publishPaymentFailed(new PaymentFailedEvent(payment.getOrderId(), "Payment was refused"));
        }

        return new PaymentResponse(payment.getId(), payment.getOrderId(), payment.getStatus().name(),
                payment.getAmount());
    }
}