package fr.guezrou.ecommerce.stock.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

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
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockService {

    private final StockRepository stockRepository;
    private final StockReservationRepository stockReservationRepository;

    @Transactional
    public ReserveStockResponse reserveStock(ReserveStockRequest request) {

        List<StockReservationEntity> existingReservations = stockReservationRepository
                .findByOrderId(request.getOrderId());

        if (!existingReservations.isEmpty()) {
            StockReservationStatus existingStatus = existingReservations.get(0).getStatus();
            log.info("Réservation déjà connue : orderId={}, status={}. Aucun nouveau mouvement de stock.",
                    request.getOrderId(), existingStatus);
            return new ReserveStockResponse(request.getOrderId(), existingStatus.name(),
                    "Reservation already processed; stock was not modified");
        }

        // Pas de réservation partielle
        for (StockReservationItemRequest item : request.getItems()) {

            StockEntity inventory = stockRepository.findByProductId(item.getProductId())
                    .orElseThrow(() -> new StockNotFoundException(item.getProductId()));
            if (inventory.getAvailableQuantity() < item.getQuantity()) {
                throw new InsufficientStockException(item.getProductId(), item.getQuantity(),
                        inventory.getAvailableQuantity());
            }
        }

        for (StockReservationItemRequest item : request.getItems()) {

            StockEntity inventory = stockRepository.findByProductId(item.getProductId())
                    .orElseThrow(() -> new StockNotFoundException(item.getProductId()));

            inventory.setAvailableQuantity(inventory.getAvailableQuantity() - item.getQuantity());

            inventory.setReservedQuantity(inventory.getReservedQuantity() + item.getQuantity());

            inventory.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));

            stockRepository.save(inventory);

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

    @Transactional
    public void releaseStock(Long orderId) {

        List<StockReservationEntity> reservations = stockReservationRepository.findByOrderId(orderId);

        if (reservations.isEmpty()) {
            log.warn("Aucune réservation trouvée : orderId={}", orderId);
            return;
        }

        for (StockReservationEntity reservation : reservations) {

            if (reservation.getStatus() != StockReservationStatus.RESERVED) {
                continue;
            }

            StockEntity inventory = stockRepository.findByProductId(reservation.getProductId())
                    .orElseThrow(() -> new StockNotFoundException(reservation.getProductId()));

            inventory.setReservedQuantity(inventory.getReservedQuantity() - reservation.getQuantity());

            inventory.setAvailableQuantity(inventory.getAvailableQuantity() + reservation.getQuantity());

            inventory.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));

            reservation.setStatus(StockReservationStatus.RELEASED);

            stockRepository.save(inventory);
            stockReservationRepository.save(reservation);
        }
    }
}
