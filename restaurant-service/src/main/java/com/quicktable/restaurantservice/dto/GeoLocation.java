package com.quicktable.restaurantservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeoLocation {
    private Double latitude;
    private Double longitude;
    private String formattedAddress;
    private String city;
    private String country;
}
