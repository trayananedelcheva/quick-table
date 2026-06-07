package com.quicktable.reservationservicev2.dto;

import com.quicktable.common.dto.ReservationStatus;
import com.quicktable.common.dto.TableLocation;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Builder
public class ReservationResponse {

    private Long id;
    private Long userId;
    private Long restaurantId;
    private String restaurantName;
    private String restaurantImageUrl;
    private Long tableId;
    private LocalDate reservationDate;
    private LocalTime reservationTime;
    private Integer numberOfGuests;
    private ReservationStatus status;
    private TableLocation preferredLocation;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private String specialRequests;
    private LocalDateTime createdAt;
}
