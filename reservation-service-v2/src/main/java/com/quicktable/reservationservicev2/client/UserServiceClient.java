package com.quicktable.reservationservicev2.client;

import com.quicktable.reservationservicev2.dto.UserDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceClient {

    @Qualifier("userServiceWebClient")
    private final WebClient userServiceWebClient;

    public UserDTO getUserById(Long userId, String token) {
        try {
            return userServiceWebClient.get()
                    .uri("/api/users/{id}", userId)
                    .header("Authorization", token)
                    .retrieve()
                    .bodyToMono(UserDTO.class)
                    .block();
        } catch (Exception e) {
            log.error("Грешка при извличане на потребител {}: {}", userId, e.getMessage());
            throw new RuntimeException("Не може да се свърже с user-service");
        }
    }
}
