package com.quicktable.reservationservicev2.dto;

import com.quicktable.common.dto.TableLocation;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
public class ReservationRequest {

    @NotNull
    private Long restaurantId;

    @NotNull
    @FutureOrPresent
    private LocalDate reservationDate;

    @NotNull
    private LocalTime reservationTime;

    @NotNull
    @Min(1)
    private Integer guestsCount;

    private TableLocation preferredLocation;

    private String specialRequests;
}
