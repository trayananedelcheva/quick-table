package com.quicktable.reservationservice.client;

import com.quicktable.common.dto.TableLocation;
import com.quicktable.reservationservice.dto.TableDTO;
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

    /**
     * Извлича всички маси за даден ресторант от restaurant-service
     */
    public List<TableDTO> getRestaurantTables(Long restaurantId) {
        log.info("Извикване на restaurant-service за маси на ресторант {}", restaurantId);
        
        try {
            List<TableDTO> tables = restaurantServiceWebClient.get()
                    .uri("/api/restaurants/{id}/tables", restaurantId)
                    .retrieve()
                    .bodyToFlux(TableDTO.class)
                    .collectList()
                    .block();
            
            log.info("Получени {} маси от restaurant-service", tables != null ? tables.size() : 0);
            return tables;
        } catch (Exception e) {
            log.error("Грешка при комуникация с restaurant-service: {}", e.getMessage());
            throw new RuntimeException("Не може да се свърже с restaurant-service: " + e.getMessage());
        }
    }

    /**
     * Филтрира маси по капацитет, налично състояние и локация
     */
    public List<TableDTO> findAvailableTables(Long restaurantId, Integer guestsCount, TableLocation location) {
        List<TableDTO> allTables = getRestaurantTables(restaurantId);
        
        return allTables.stream()
                .filter(table -> table.getAvailable() != null && table.getAvailable())
                .filter(table -> table.getCapacity() >= guestsCount)
                .filter(table -> location == null || table.getLocation() == location)
                .toList();
    }
}
