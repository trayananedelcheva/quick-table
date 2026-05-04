package com.quicktable.reservationservicev2.controller;

import com.quicktable.common.dto.UserRole;
import com.quicktable.reservationservicev2.dto.ReservationRequest;
import com.quicktable.reservationservicev2.dto.ReservationResponse;
import com.quicktable.reservationservicev2.security.SecurityUtils;
import com.quicktable.reservationservicev2.service.ReservationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;
    private final SecurityUtils securityUtils;

    @PostMapping
    public ResponseEntity<ReservationResponse> createReservation(
            @Valid @RequestBody ReservationRequest request,
            HttpServletRequest httpRequest
    ) {
        Long userId = securityUtils.getCurrentUserId();
        UserRole userRole = UserRole.valueOf(securityUtils.getCurrentUserRole());
        String token = httpRequest.getHeader("Authorization");

        ReservationResponse response = reservationService.createReservation(userId, userRole, token, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReservationResponse> getReservationById(@PathVariable Long id) {
        ReservationResponse response = reservationService.getReservationById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my")
    public ResponseEntity<List<ReservationResponse>> getMyReservations() {
        Long userId = securityUtils.getCurrentUserId();
        List<ReservationResponse> reservations = reservationService.getMyReservations(userId);
        return ResponseEntity.ok(reservations);
    }
}
