package com.quicktable.restaurantservice.controller;

import com.quicktable.restaurantservice.dto.ReviewRequest;
import com.quicktable.restaurantservice.dto.ReviewResponse;
import com.quicktable.restaurantservice.service.ReviewService;
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

    @PostMapping
    public ResponseEntity<ReviewResponse> addReview(
            HttpServletRequest request,
            @PathVariable Long restaurantId,
            @Valid @RequestBody ReviewRequest reviewRequest
    ) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String firstName = (String) request.getAttribute("firstName");
        String lastName = (String) request.getAttribute("lastName");
        String customerName = (firstName != null && lastName != null)
                ? firstName + " " + lastName
                : "Анонимен";
        String token = request.getHeader("Authorization");

        ReviewResponse response = reviewService.addReview(restaurantId, reviewRequest, userId, customerName, token);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ReviewResponse>> getReviews(@PathVariable Long restaurantId) {
        return ResponseEntity.ok(reviewService.getReviewsByRestaurant(restaurantId));
    }
}
