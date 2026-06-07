package com.quicktable.reservationservicev2.controller;

import com.quicktable.common.dto.ReservationStatus;
import com.quicktable.common.dto.UserRole;
import com.quicktable.common.dto.TableLocation;
import com.quicktable.reservationservicev2.dto.AdminReservationRequest;
import com.quicktable.reservationservicev2.dto.RejectReservationRequest;
import com.quicktable.reservationservicev2.dto.ReservationRequest;
import com.quicktable.reservationservicev2.dto.ReservationResponse;
import com.quicktable.reservationservicev2.dto.TimeSlotResponse;
import com.quicktable.reservationservicev2.security.SecurityUtils;
import com.quicktable.reservationservicev2.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;
    private final SecurityUtils securityUtils;

    @Operation(summary = "[Client] Create reservation", tags = {"Client"})
    @SecurityRequirement(name = "Client")
    @PostMapping
    public ResponseEntity<ReservationResponse> createReservation(
            @Valid @RequestBody ReservationRequest request,
            @RequestHeader("Authorization") String token
    ) {
        Long userId = securityUtils.getCurrentUserId();
        UserRole userRole = UserRole.valueOf(securityUtils.getCurrentUserRole());

        ReservationResponse response = reservationService.createReservation(userId, userRole, token, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "[System Admin] Create reservation on behalf of client", tags = {"System Admin"})
    @SecurityRequirement(name = "System Admin")
    @PostMapping("/admin")
    public ResponseEntity<ReservationResponse> createReservationAsAdmin(
            @Valid @RequestBody AdminReservationRequest request,
            @RequestHeader("Authorization") String token
    ) {
        UserRole userRole = UserRole.valueOf(securityUtils.getCurrentUserRole());
        if (userRole != UserRole.SYSTEM_ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Long adminId = securityUtils.getCurrentUserId();
        ReservationResponse response = reservationService.createReservationAsAdmin(adminId, token, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "[Client] Get reservation by ID", tags = {"Client"})
    @SecurityRequirement(name = "Client")
    @GetMapping("/{id}")
    public ResponseEntity<ReservationResponse> getReservationById(@PathVariable Long id) {
        ReservationResponse response = reservationService.getReservationById(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "[Client] Get my reservations", tags = {"Client"})
    @SecurityRequirement(name = "Client")
    @GetMapping("/my")
    public ResponseEntity<List<ReservationResponse>> getMyReservations(
            @RequestParam(required = false, defaultValue = "false") boolean upcoming,
            @RequestParam(required = false) ReservationStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate
    ) {
        Long userId = securityUtils.getCurrentUserId();
        List<ReservationResponse> reservations = upcoming
                ? reservationService.getMyUpcomingReservations(userId)
                : reservationService.getMyReservations(userId, status, fromDate);
        return ResponseEntity.ok(reservations);
    }

    @Operation(summary = "[System Admin] Get all reservations", tags = {"System Admin"})
    @SecurityRequirement(name = "System Admin")
    @GetMapping
    public ResponseEntity<List<ReservationResponse>> getAllReservations() {
        UserRole userRole = UserRole.valueOf(securityUtils.getCurrentUserRole());
        if (userRole != UserRole.SYSTEM_ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(reservationService.getAllReservations());
    }

    @Operation(summary = "[Client] Cancel reservation", tags = {"Client"})
    @SecurityRequirement(name = "Client")
    @DeleteMapping("/{id}")
    public ResponseEntity<ReservationResponse> cancelReservation(@PathVariable Long id) {
        Long userId = securityUtils.getCurrentUserId();
        UserRole userRole = UserRole.valueOf(securityUtils.getCurrentUserRole());
        ReservationResponse response = reservationService.cancelReservation(id, userId, userRole);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "[Restaurant Admin] Reject reservation", tags = {"Restaurant Admin"})
    @SecurityRequirement(name = "Restaurant Admin")
    @PatchMapping("/{id}/reject")
    public ResponseEntity<ReservationResponse> rejectReservation(
            @PathVariable Long id,
            @Valid @RequestBody RejectReservationRequest request
    ) {
        Long adminId = securityUtils.getCurrentUserId();
        UserRole userRole = UserRole.valueOf(securityUtils.getCurrentUserRole());
        ReservationResponse response = reservationService.rejectReservation(id, adminId, userRole, request.getReason());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "[Restaurant Admin] Complete reservation", tags = {"Restaurant Admin"})
    @SecurityRequirement(name = "Restaurant Admin")
    @PatchMapping("/{id}/complete")
    public ResponseEntity<ReservationResponse> completeReservation(@PathVariable Long id) {
        UserRole userRole = UserRole.valueOf(securityUtils.getCurrentUserRole());
        ReservationResponse response = reservationService.completeReservation(id, userRole);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "[Restaurant Admin] Mark reservation as no-show", tags = {"Restaurant Admin"})
    @SecurityRequirement(name = "Restaurant Admin")
    @PatchMapping("/{id}/no-show")
    public ResponseEntity<ReservationResponse> markNoShow(@PathVariable Long id) {
        UserRole userRole = UserRole.valueOf(securityUtils.getCurrentUserRole());
        ReservationResponse response = reservationService.markNoShow(id, userRole);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "[Restaurant Admin] Get restaurant reservations", tags = {"Restaurant Admin"})
    @SecurityRequirement(name = "Restaurant Admin")
    @GetMapping("/restaurant/{restaurantId}")
    public ResponseEntity<List<ReservationResponse>> getRestaurantReservations(
            @PathVariable Long restaurantId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) ReservationStatus status
    ) {
        Long userId = securityUtils.getCurrentUserId();
        UserRole userRole = UserRole.valueOf(securityUtils.getCurrentUserRole());
        List<ReservationResponse> reservations = reservationService.getRestaurantReservations(restaurantId, date, status, userId, userRole);
        return ResponseEntity.ok(reservations);
    }

    @Operation(summary = "[Client] Get available time slots", tags = {"Client"})
    @SecurityRequirement(name = "Client")
    @GetMapping("/restaurant/{restaurantId}/available-slots")
    public ResponseEntity<List<TimeSlotResponse>> getAvailableTimeSlots(
            @PathVariable Long restaurantId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam Integer guestsCount,
            @RequestParam(required = false) TableLocation location
    ) {
        List<TimeSlotResponse> slots = reservationService.getAvailableTimeSlots(restaurantId, date, guestsCount, location);
        return ResponseEntity.ok(slots);
    }
}
