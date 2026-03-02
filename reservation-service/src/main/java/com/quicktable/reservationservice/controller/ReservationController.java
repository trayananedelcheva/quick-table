package com.quicktable.reservationservice.controller;

import com.quicktable.common.dto.ReservationStatus;
import com.quicktable.common.dto.TableCategory;
import com.quicktable.reservationservice.dto.ReservationRequest;
import com.quicktable.reservationservice.dto.ReservationResponse;
import com.quicktable.reservationservice.dto.UpdateReservationStatusRequest;
import com.quicktable.reservationservice.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping
    public ResponseEntity<ReservationResponse> createReservation(
            @RequestParam Long userId,
            @RequestParam String userRole, // CLIENT, RESTAURANT_ADMIN, SYSTEM_ADMIN
            @Valid @RequestBody ReservationRequest request
    ) {
        // Добавяме ролята за бизнес валидация
        request.setUserRole(com.quicktable.common.dto.UserRole.valueOf(userRole.toUpperCase()));
        ReservationResponse response = reservationService.createReservation(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReservationResponse> getReservationById(@PathVariable Long id) {
        ReservationResponse response = reservationService.getReservationById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my")
    public ResponseEntity<List<ReservationResponse>> getMyReservations(
            @RequestParam Long userId
    ) {
        List<ReservationResponse> reservations = reservationService.getMyReservations(userId);
        return ResponseEntity.ok(reservations);
    }

    @GetMapping("/restaurant/{restaurantId}")
    public ResponseEntity<List<ReservationResponse>> getRestaurantReservations(
            @PathVariable Long restaurantId,
            @RequestParam Long adminUserId,
            @RequestParam(required = false) ReservationStatus status
    ) {
        List<ReservationResponse> reservations = status != null
                ? reservationService.getRestaurantReservationsByStatus(restaurantId, status)
                : reservationService.getRestaurantReservations(restaurantId, adminUserId);
        return ResponseEntity.ok(reservations);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ReservationResponse> updateReservationStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateReservationStatusRequest request
    ) {
        ReservationResponse response = reservationService.updateReservationStatus(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelReservation(@PathVariable Long id) {
        reservationService.cancelReservation(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/restaurant/{restaurantId}/availability")
    public ResponseEntity<List<ReservationResponse>> getAvailability(
            @PathVariable Long restaurantId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        List<ReservationResponse> reservations = reservationService.getRestaurantReservations(restaurantId, null);
        return ResponseEntity.ok(reservations);
    }

    @GetMapping("/restaurant/{restaurantId}/available-slots")
    public ResponseEntity<List<String>> getAvailableTimeSlots(
            @PathVariable Long restaurantId,
            @RequestParam String date, // YYYY-MM-DD
            @RequestParam Integer guestsCount,
            @RequestParam(required = false) String category // "INSIDE", "SUMMER_GARDEN", "WINTER_GARDEN"
    ) {
        java.time.LocalDate reservationDate = java.time.LocalDate.parse(date);
        
        TableCategory tableCategory = null;
        if (category != null && !category.isEmpty()) {
            try {
                tableCategory = TableCategory.valueOf(category.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().build();
            }
        }
        
        List<String> availableSlots = reservationService.getAvailableTimeSlots(
                restaurantId, reservationDate, guestsCount, tableCategory);
        return ResponseEntity.ok(availableSlots);
    }
}
