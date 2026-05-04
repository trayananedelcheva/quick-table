package com.quicktable.restaurantservice.repository;

import com.quicktable.restaurantservice.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByRestaurantIdOrderByCreatedAtDesc(Long restaurantId);

    Optional<Review> findByReservationId(Long reservationId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.restaurant.id = :restaurantId")
    Double findAverageRatingByRestaurantId(@Param("restaurantId") Long restaurantId);

    long countByRestaurantId(Long restaurantId);
}
