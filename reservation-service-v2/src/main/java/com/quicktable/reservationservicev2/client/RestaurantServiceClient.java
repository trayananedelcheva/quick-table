package com.quicktable.reservationservicev2.client;

import com.quicktable.common.dto.TableLocation;
import com.quicktable.reservationservicev2.dto.RestaurantDTO;
import com.quicktable.reservationservicev2.dto.TableDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RestaurantServiceClient {

    private final WebClient restaurantServiceWebClient;

    public RestaurantDTO getRestaurantById(Long restaurantId) {
        try {
            return restaurantServiceWebClient.get()
                    .uri("/api/restaurants/{id}", restaurantId)
                    .retrieve()
                    .bodyToMono(RestaurantDTO.class)
                    .block();
        } catch (Exception e) {
            log.error("Грешка при извличане на ресторант {}: {}", restaurantId, e.getMessage());
            return RestaurantDTO.builder().id(restaurantId).name("Ресторант #" + restaurantId).build();
        }
    }

    public List<TableDTO> findTablesByCapacityAndLocation(Long restaurantId, Integer guestsCount, TableLocation location) {
        try {
            List<TableDTO> allTables = restaurantServiceWebClient.get()
                    .uri("/api/restaurants/{id}/tables", restaurantId)
                    .retrieve()
                    .bodyToFlux(TableDTO.class)
                    .collectList()
                    .block();

            if (allTables == null) return List.of();

            return allTables.stream()
                    .filter(t -> Boolean.TRUE.equals(t.getAvailable()))
                    .filter(t -> t.getCapacity() >= guestsCount)
                    .filter(t -> location == null || t.getLocation() == location)
                    .toList();
        } catch (Exception e) {
            log.error("Грешка при извличане на маси за ресторант {}: {}", restaurantId, e.getMessage());
            throw new RuntimeException("Не може да се свърже с restaurant-service");
        }
    }
}
