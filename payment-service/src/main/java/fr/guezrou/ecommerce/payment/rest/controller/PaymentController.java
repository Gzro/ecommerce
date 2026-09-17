package fr.guezrou.ecommerce.payment.rest.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fr.guezrou.ecommerce.payment.rest.request.PaymentRequest;
import fr.guezrou.ecommerce.payment.rest.response.PaymentResponse;
import fr.guezrou.ecommerce.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("")
    public ResponseEntity<PaymentResponse> processPayment(@RequestBody PaymentRequest request) {

        PaymentResponse response = paymentService.processPayment(request);

        return ResponseEntity.ok(response);
    }
}