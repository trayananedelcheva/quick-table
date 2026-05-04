package com.quicktable.restaurantservice.client;

import com.quicktable.restaurantservice.dto.ReservationDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationServiceClient {

    private final WebClient reservationServiceWebClient;

    public ReservationDTO getReservationById(Long reservationId, String token) {
        try {
            return reservationServiceWebClient.get()
                    .uri("/api/reservations/{id}", reservationId)
                    .header("Authorization", token)
                    .retrieve()
                    .bodyToMono(ReservationDTO.class)
                    .block();
        } catch (WebClientResponseException.NotFound e) {
            return null;
        } catch (Exception e) {
            log.error("Грешка при извличане на резервация {}: {}", reservationId, e.getMessage());
            throw new RuntimeException("Не може да се свърже с reservation-service");
        }
    }
}
