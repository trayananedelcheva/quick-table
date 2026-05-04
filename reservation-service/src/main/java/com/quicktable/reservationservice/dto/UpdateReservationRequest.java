package com.quicktable.reservationservice.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateReservationRequest {

    @NotNull(message = "Датата е задължителна")
    @FutureOrPresent(message = "Датата не може да е в миналото")
    private LocalDate reservationDate;

    @NotNull(message = "Часът е задължителен")
    private LocalTime reservationTime;

    @NotNull(message = "Броят гости е задължителен")
    @Min(value = 1, message = "Броят гости трябва да е поне 1")
    private Integer guestsCount;

    private String specialRequests;
}
