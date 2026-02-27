package com.quicktable.restaurantservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO за отговор от Nominatim API (OpenStreetMap)
 * Документация: https://nominatim.org/release-docs/latest/api/Search/
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NominatimResponse {

    @JsonProperty("place_id")
    private Long placeId;

    @JsonProperty("lat")
    private String latitude;

    @JsonProperty("lon")
    private String longitude;

    @JsonProperty("display_name")
    private String displayName;

    @JsonProperty("address")
    private AddressDetails address;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddressDetails {
        private String road;
        private String city;
        private String town;
        private String village;
        private String state;
        private String country;
        
        @JsonProperty("country_code")
        private String countryCode;
    }
}
