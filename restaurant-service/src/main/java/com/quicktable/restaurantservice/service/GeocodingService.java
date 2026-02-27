package com.quicktable.restaurantservice.service;

import com.quicktable.restaurantservice.dto.GeoLocation;
import com.quicktable.restaurantservice.dto.NominatimResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Външна услуга за геокодиране с OpenStreetMap Nominatim API
 * Документация: https://nominatim.org/release-docs/latest/api/Search/
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class GeocodingService {

    private final WebClient.Builder webClientBuilder;
    private static final String NOMINATIM_API_URL = "https://nominatim.openstreetmap.org";

    /**
     * Геокодира адрес към географски координати
     * @param address Пълен адрес за геокодиране
     * @return GeoLocation с координати и форматиран адрес
     */
    public GeoLocation geocodeAddress(String address) {
        log.info("Геокодиране на адрес: {}", address);
        
        try {
            WebClient webClient = webClientBuilder.baseUrl(NOMINATIM_API_URL).build();
            
            List<NominatimResponse> responses = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/search")
                            .queryParam("q", address)
                            .queryParam("format", "json")
                            .queryParam("addressdetails", "1")
                            .queryParam("limit", "1")
                            .build())
                    .header("User-Agent", "QuickTable/1.0")
                    .retrieve()
                    .bodyToMono(NominatimResponse[].class)
                    .map(array -> array.length > 0 ? List.of(array) : List.<NominatimResponse>of())
                    .block();

            if (responses != null && !responses.isEmpty()) {
                NominatimResponse response = responses.get(0);
                log.info("Успешно геокодиране: lat={}, lon={}", response.getLatitude(), response.getLongitude());
                
                String city = extractCity(response);
                String country = response.getAddress() != null ? response.getAddress().getCountry() : null;
                
                return GeoLocation.builder()
                        .latitude(Double.parseDouble(response.getLatitude()))
                        .longitude(Double.parseDouble(response.getLongitude()))
                        .formattedAddress(response.getDisplayName())
                        .city(city)
                        .country(country)
                        .build();
            }
            
            log.warn("Не са намерени резултати за адрес: {}", address);
            return null;
            
        } catch (Exception e) {
            log.error("Грешка при геокодиране на адрес: {}", address, e);
            return null;
        }
    }

    /**
     * Обратно геокодиране - от координати към адрес
     * @param latitude Географска ширина
     * @param longitude Географска дължина
     * @return GeoLocation с адресна информация
     */
    public GeoLocation reverseGeocode(Double latitude, Double longitude) {
        log.info("Обратно геокодиране: lat={}, lon={}", latitude, longitude);
        
        try {
            WebClient webClient = webClientBuilder.baseUrl(NOMINATIM_API_URL).build();
            
            NominatimResponse response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/reverse")
                            .queryParam("lat", latitude)
                            .queryParam("lon", longitude)
                            .queryParam("format", "json")
                            .queryParam("addressdetails", "1")
                            .build())
                    .header("User-Agent", "QuickTable/1.0")
                    .retrieve()
                    .bodyToMono(NominatimResponse.class)
                    .block();

            if (response != null) {
                String city = extractCity(response);
                String country = response.getAddress() != null ? response.getAddress().getCountry() : null;
                
                return GeoLocation.builder()
                        .latitude(latitude)
                        .longitude(longitude)
                        .formattedAddress(response.getDisplayName())
                        .city(city)
                        .country(country)
                        .build();
            }
            
            log.warn("Не са намерени резултати за координати: {}, {}", latitude, longitude);
            return null;
            
        } catch (Exception e) {
            log.error("Грешка при обратно геокодиране", e);
            return null;
        }
    }

    private String extractCity(NominatimResponse response) {
        if (response.getAddress() != null) {
            NominatimResponse.AddressDetails address = response.getAddress();
            if (address.getCity() != null) return address.getCity();
            if (address.getTown() != null) return address.getTown();
            if (address.getVillage() != null) return address.getVillage();
        }
        return null;
    }
}
