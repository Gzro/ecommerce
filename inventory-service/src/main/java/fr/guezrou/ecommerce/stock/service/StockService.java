package fr.guezrou.ecommerce.stock.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fr.guezrou.ecommerce.stock.db.entity.StockEntity;
import fr.guezrou.ecommerce.stock.db.entity.StockReservationEntity;
import fr.guezrou.ecommerce.stock.db.entity.StockReservationStatus;
import fr.guezrou.ecommerce.stock.db.repository.StockRepository;
import fr.guezrou.ecommerce.stock.db.repository.StockReservationRepository;
import fr.guezrou.ecommerce.stock.exceptions.InsufficientStockException;
import fr.guezrou.ecommerce.stock.exceptions.StockNotFoundException;
import fr.guezrou.ecommerce.stock.rest.request.ReserveStockRequest;
import fr.guezrou.ecommerce.stock.rest.request.StockReservationItemRequest;
import fr.guezrou.ecommerce.stock.rest.response.ReserveStockResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StockService {

    private final StockRepository inventoryRepository;
    private final StockReservationRepository stockReservationRepository;

    @Transactional
    public ReserveStockResponse reserveStock(ReserveStockRequest request) {

        // Pas de réservation partielle
        for (StockReservationItemRequest item : request.getItems()) {

            StockEntity inventory = inventoryRepository.findByProductId(item.getProductId())
                    .orElseThrow(() -> new StockNotFoundException(item.getProductId()));
            if (inventory.getAvailableQuantity() < item.getQuantity()) {
                throw new InsufficientStockException(item.getProductId(), item.getQuantity(),
                        inventory.getAvailableQuantity());
            }
        }

        for (StockReservationItemRequest item : request.getItems()) {

            StockEntity inventory = inventoryRepository.findByProductId(item.getProductId())
                    .orElseThrow(() -> new StockNotFoundException(item.getProductId()));

            inventory.setAvailableQuantity(inventory.getAvailableQuantity() - item.getQuantity());

            inventory.setReservedQuantity(inventory.getReservedQuantity() + item.getQuantity());

            inventory.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));

            inventoryRepository.save(inventory);

            StockReservationEntity reservation = new StockReservationEntity();

            reservation.setOrderId(request.getOrderId());
            reservation.setProductId(item.getProductId());
            reservation.setQuantity(item.getQuantity());
            reservation.setStatus(StockReservationStatus.RESERVED);
            reservation.setCreatedAt(LocalDateTime.now());

            stockReservationRepository.save(reservation);
        }

        return new ReserveStockResponse(request.getOrderId(), "RESERVED", "Stock successfully reserved");
    }
}
