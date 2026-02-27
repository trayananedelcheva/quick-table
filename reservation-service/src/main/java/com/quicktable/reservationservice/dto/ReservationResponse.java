package com.quicktable.reservationservice.dto;

import com.quicktable.common.dto.ReservationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationResponse {

    private Long id;
    private Long userId;
    private Long restaurantId;
    private Long tableId;
    private LocalDate reservationDate;
    private LocalTime reservationTime;
    private Integer numberOfGuests;
    private ReservationStatus status;
    private String specialRequests;
    private String customerName;
    private String customerPhone;
    private String customerEmail;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Допълнителна информация от други услуги (опционално)
    private String restaurantName;
    private String tableNumber;
}
