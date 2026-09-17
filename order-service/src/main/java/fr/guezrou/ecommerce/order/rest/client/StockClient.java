package fr.guezrou.ecommerce.order.rest.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import fr.guezrou.ecommerce.order.exceptions.StockServiceException;
import fr.guezrou.ecommerce.order.rest.request.ReserveStockRequest;
import fr.guezrou.ecommerce.order.rest.response.ReserveStockResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class StockClient {

    private static final String ENDPOINT_API_STOCK_RESERVATIONS = "http://host.docker.internal:8081/api/v1/stock/reservations";
    private final RestClient restClient;

    public ReserveStockResponse reserveStock(ReserveStockRequest request) {

        try {

            return restClient.post().uri(ENDPOINT_API_STOCK_RESERVATIONS).body(request).retrieve()
                    .body(ReserveStockResponse.class);

        } catch (RestClientResponseException exception) {

            throw new StockServiceException("Unable to reserve stock for order " + request.getOrderId(), exception);
        }
    }
}
