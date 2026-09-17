package fr.guezrou.ecommerce.order.rest.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import fr.guezrou.ecommerce.order.exceptions.StockServiceException;
import fr.guezrou.ecommerce.order.rest.request.PaymentRequest;
import fr.guezrou.ecommerce.order.rest.response.PaymentResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PaymentClient {

    private static final String ENDPOINT_API_STOCK_PAYMENT = "http://host.docker.internal:8082/api/v1/payments";
    private final RestClient restClient;

    public PaymentResponse pay(PaymentRequest request) {

        try {

            return restClient.post().uri(ENDPOINT_API_STOCK_PAYMENT).body(request).retrieve()
                    .body(PaymentResponse.class);

        } catch (RestClientResponseException exception) {

            throw new StockServiceException("Unable to pay for order " + request.getOrderId(), exception);
        }
    }
}
