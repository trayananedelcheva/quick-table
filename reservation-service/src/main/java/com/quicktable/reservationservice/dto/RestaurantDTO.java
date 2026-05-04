package com.quicktable.reservationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

/**
 * DTO за информация за ресторант от restaurant-service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantDTO {
    private Long id;
    private String name;
    private String address;
    private String phoneNumber;
    private String email;
    private String description;
    private Long ownerId;
    private LocalTime openingTime;
    private LocalTime closingTime;
}
