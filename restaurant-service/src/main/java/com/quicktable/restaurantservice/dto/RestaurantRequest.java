package com.quicktable.restaurantservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantRequest {

    @NotBlank(message = "Името е задължително")
    private String name;

    private String description;

    @NotBlank(message = "Адресът е задължителен")
    private String address;

    private String city;

    private String country;

    private String phone;

    private String email;

    @NotNull(message = "Времето за отваряне е задължително")
    private LocalTime openingTime;

    @NotNull(message = "Времето за затваряне е задължително")
    private LocalTime closingTime;

    private Long adminUserId;

    private List<TableRequest> tables;
}
