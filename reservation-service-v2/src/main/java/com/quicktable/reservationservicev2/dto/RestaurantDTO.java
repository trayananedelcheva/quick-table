package com.quicktable.reservationservicev2.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantDTO {
    private Long id;
    private String name;
    private Long ownerId;
    private LocalTime openingTime;
    private LocalTime closingTime;
}
