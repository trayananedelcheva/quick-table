package com.quicktable.restaurantservice.controller;

import com.quicktable.restaurantservice.dto.ReviewRequest;
import com.quicktable.restaurantservice.dto.ReviewResponse;
import com.quicktable.restaurantservice.security.SecurityUtils;
import com.quicktable.restaurantservice.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/restaurants/{restaurantId}/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final SecurityUtils securityUtils;

    @Operation(summary = "[Client] Add review for restaurant", tags = {"Client"})
    @SecurityRequirement(name = "Client")
    @PostMapping
    public ResponseEntity<ReviewResponse> addReview(
            HttpServletRequest httpRequest,
            @PathVariable Long restaurantId,
            @Valid @RequestBody ReviewRequest reviewRequest
    ) {
        Long userId = securityUtils.getCurrentUserId();
        String firstName = securityUtils.getCurrentUserFirstName();
        String lastName = securityUtils.getCurrentUserLastName();
        String customerName = (firstName != null && lastName != null)
                ? firstName + " " + lastName
                : "Анонимен";
        String token = httpRequest.getHeader("Authorization");

        ReviewResponse response = reviewService.addReview(restaurantId, reviewRequest, userId, customerName, token);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "[Public] Get reviews for restaurant", tags = {"Public"})
    @GetMapping
    public ResponseEntity<List<ReviewResponse>> getReviews(@PathVariable Long restaurantId) {
        return ResponseEntity.ok(reviewService.getReviewsByRestaurant(restaurantId));
    }

    @Operation(summary = "[Client] Get review by reservation ID", tags = {"Client"})
    @GetMapping("/by-reservation/{reservationId}")
    public ResponseEntity<ReviewResponse> getReviewByReservation(
            @PathVariable Long restaurantId,
            @PathVariable Long reservationId
    ) {
        return reviewService.getReviewByReservationId(reservationId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
