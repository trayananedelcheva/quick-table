package com.quicktable.reservationservicev2.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class RejectReservationRequest {

    @NotBlank(message = "Причината за отказ не може да бъде празна.")
    @Size(max = 500, message = "Причината не може да надвишава 500 символа.")
    private String reason;
}
