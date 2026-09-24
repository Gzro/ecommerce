package fr.guezrou.ecommerce.payment.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fr.guezrou.ecommerce.payment.db.entity.PaymentEntity;
import fr.guezrou.ecommerce.payment.db.entity.PaymentStatus;
import fr.guezrou.ecommerce.payment.db.repository.PaymentRepository;
import fr.guezrou.ecommerce.payment.event.PaymentCompletedEvent;
import fr.guezrou.ecommerce.payment.event.PaymentEventPublisher;
import fr.guezrou.ecommerce.payment.event.PaymentFailedEvent;
import fr.guezrou.ecommerce.payment.rest.request.PaymentRequest;
import fr.guezrou.ecommerce.payment.rest.response.PaymentResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentEventPublisher paymentEventPublisher;

    @Transactional
    public PaymentResponse processPayment(PaymentRequest request) {

        PaymentEntity payment = new PaymentEntity();

        payment.setOrderId(request.getOrderId());
        payment.setCustomerId(request.getCustomerId());
        payment.setAmount(request.getAmount());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setCreatedAt(LocalDateTime.now());

        paymentRepository.save(payment);

        boolean paymentAccepted = false;

        if (paymentAccepted) {

            payment.setStatus(PaymentStatus.COMPLETED);

            paymentRepository.save(payment);

            PaymentCompletedEvent event = new PaymentCompletedEvent(payment.getOrderId(), payment.getId(),
                    payment.getAmount());

            paymentEventPublisher.publishPaymentCompleted(event);

        } else {

            payment.setStatus(PaymentStatus.FAILED);

            paymentRepository.save(payment);

            PaymentFailedEvent event = new PaymentFailedEvent(payment.getOrderId(), "Payment was refused");

            paymentEventPublisher.publishPaymentFailed(event);
        }

        return new PaymentResponse(payment.getId(), payment.getOrderId(), payment.getStatus().name(),
                payment.getAmount());
    }
}