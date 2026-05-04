package com.quicktable.restaurantservice.service;

import com.quicktable.restaurantservice.client.ReservationServiceClient;
import com.quicktable.restaurantservice.dto.ReservationDTO;
import com.quicktable.restaurantservice.dto.ReviewRequest;
import com.quicktable.restaurantservice.dto.ReviewResponse;
import com.quicktable.restaurantservice.entity.Restaurant;
import com.quicktable.restaurantservice.entity.Review;
import com.quicktable.restaurantservice.repository.RestaurantRepository;
import com.quicktable.restaurantservice.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final RestaurantRepository restaurantRepository;
    private final ReservationServiceClient reservationServiceClient;

    @Transactional
    public ReviewResponse addReview(Long restaurantId, ReviewRequest request, Long userId, String customerName, String token) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new RuntimeException("Ресторантът не е намерен"));

        // Едно посещение = максимум един review
        if (reviewRepository.findByReservationId(request.getReservationId()).isPresent()) {
            throw new RuntimeException("Вече сте оставили оценка за тази резервация");
        }

        // Валидация на резервацията
        ReservationDTO reservation = reservationServiceClient.getReservationById(request.getReservationId(), token);

        if (reservation == null) {
            throw new RuntimeException("Резервация с ID " + request.getReservationId() + " не е намерена.");
        }

        if (!reservation.getUserId().equals(userId)) {
            throw new RuntimeException("Тази резервация не принадлежи на вас.");
        }

        if (!reservation.getRestaurantId().equals(restaurantId)) {
            throw new RuntimeException("Тази резервация не е за този ресторант.");
        }

        if (!"COMPLETED".equals(reservation.getStatus())) {
            throw new RuntimeException("Можете да оставите ревю само за приключена резервация.");
        }

        // Проверка дали са минали не повече от 2 седмици от датата на резервацията
        LocalDate deadline = reservation.getReservationDate().plusWeeks(2);
        if (LocalDate.now().isAfter(deadline)) {
            throw new RuntimeException("Срокът за оставяне на ревю е изтекъл (до 2 седмици след резервацията).");
        }

        Review review = Review.builder()
                .restaurant(restaurant)
                .userId(userId)
                .reservationId(request.getReservationId())
                .customerName(customerName)
                .rating(request.getRating())
                .comment(request.getComment())
                .build();

        review = reviewRepository.save(review);
        log.info("Добавен review за ресторант {} от потребител {}", restaurantId, userId);

        return mapToResponse(review);
    }

    public List<ReviewResponse> getReviewsByRestaurant(Long restaurantId) {
        return reviewRepository.findByRestaurantIdOrderByCreatedAtDesc(restaurantId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private ReviewResponse mapToResponse(Review review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .restaurantId(review.getRestaurant().getId())
                .userId(review.getUserId())
                .reservationId(review.getReservationId())
                .customerName(review.getCustomerName())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
