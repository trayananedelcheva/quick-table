package com.quicktable.reservationservice.repository;

import com.quicktable.common.dto.ReservationStatus;
import com.quicktable.reservationservice.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    
    List<Reservation> findByUserId(Long userId);
    
    List<Reservation> findByRestaurantId(Long restaurantId);

    List<Reservation> findByRestaurantIdAndReservationDate(Long restaurantId, LocalDate reservationDate);
    
    List<Reservation> findByRestaurantIdAndStatus(Long restaurantId, ReservationStatus status);
    
    List<Reservation> findByUserIdAndStatus(Long userId, ReservationStatus status);
    
    @Query("SELECT r FROM Reservation r WHERE r.tableId = :tableId " +
           "AND r.reservationDate = :date " +
           "AND r.status IN ('PENDING', 'CONFIRMED')")
    List<Reservation> findActiveReservationsForTable(
            @Param("tableId") Long tableId,
            @Param("date") LocalDate date
    );
    
    @Query("SELECT r FROM Reservation r WHERE r.restaurantId = :restaurantId " +
           "AND r.reservationDate = :date " +
           "AND r.status IN ('PENDING', 'CONFIRMED')")
    List<Reservation> findActiveReservationsForRestaurant(
            @Param("restaurantId") Long restaurantId,
            @Param("date") LocalDate date
    );
}
