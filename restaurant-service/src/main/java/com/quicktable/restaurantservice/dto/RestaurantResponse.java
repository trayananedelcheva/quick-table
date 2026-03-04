package com.quicktable.restaurantservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantResponse {

    private Long id;
    private String name;
    private String description;
    private String address;
    private String city;
    private String country;
    private Double latitude;
    private Double longitude;
    private String phone;
    private String email;
    private LocalTime openingTime;
    private LocalTime closingTime;
    private Long adminUserId;
    private Boolean active;
    private Integer totalTables;
    private Integer availableTables;
    private Map<String, LocationGroupResponse> locations; // Маси групирани по локация
}
