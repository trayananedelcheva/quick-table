package com.quicktable.reservationservicev2.repository;

import com.quicktable.common.dto.ReservationStatus;
import com.quicktable.reservationservicev2.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    @Query("SELECT r FROM Reservation r WHERE r.restaurantId = :restaurantId " +
           "AND r.reservationDate = :date " +
           "AND r.status = 'CONFIRMED'")
    List<Reservation> findActiveReservationsForRestaurant(
            @Param("restaurantId") Long restaurantId,
            @Param("date") LocalDate date
    );

    List<Reservation> findByUserIdOrderByReservationDateDesc(Long userId);

    @Query("SELECT r FROM Reservation r WHERE r.userId = :userId " +
           "AND r.reservationDate = :date " +
           "AND r.status = 'CONFIRMED'")
    List<Reservation> findConfirmedReservationsByUserAndDate(
            @Param("userId") Long userId,
            @Param("date") LocalDate date
    );

    List<Reservation> findByRestaurantIdOrderByReservationDateAscReservationTimeAsc(Long restaurantId);

    List<Reservation> findByRestaurantIdAndReservationDateOrderByReservationTimeAsc(Long restaurantId, LocalDate date);

    List<Reservation> findByRestaurantIdAndStatusOrderByReservationDateAscReservationTimeAsc(Long restaurantId, ReservationStatus status);

    List<Reservation> findByRestaurantIdAndReservationDateAndStatusOrderByReservationTimeAsc(Long restaurantId, LocalDate date, ReservationStatus status);

    @Query("SELECT r FROM Reservation r WHERE r.status = 'CONFIRMED' " +
           "AND (r.reservationDate < :today OR (r.reservationDate = :today AND r.reservationTime <= :cutoffTime))")
    List<Reservation> findReservationsToAutoComplete(
            @Param("today") LocalDate today,
            @Param("cutoffTime") LocalTime cutoffTime
    );
}
