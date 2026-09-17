package fr.guezrou.ecommerce.stock.rest.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fr.guezrou.ecommerce.stock.rest.request.ReserveStockRequest;
import fr.guezrou.ecommerce.stock.rest.response.ReserveStockResponse;
import fr.guezrou.ecommerce.stock.service.StockService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/stock")
@RequiredArgsConstructor
public class StockReservationController {

    private final StockService inventoryService;

    @PostMapping("/reservations")
    public ResponseEntity<ReserveStockResponse> reserveStock(@RequestBody ReserveStockRequest request) {

        ReserveStockResponse response = inventoryService.reserveStock(request);

        return ResponseEntity.ok(response);
    }
}
