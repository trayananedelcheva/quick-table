package com.quicktable.restaurantservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservationDTO {
    private Long id;
    private Long userId;
    private Long restaurantId;
    private LocalDate reservationDate;
    private LocalTime reservationTime;
    private String status;
    private LocalDateTime createdAt;
}
