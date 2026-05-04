package com.quicktable.reservationservicev2.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${restaurant-service.url}")
    private String restaurantServiceUrl;

    @Value("${user-service.url}")
    private String userServiceUrl;

    @Bean
    public WebClient restaurantServiceWebClient() {
        return WebClient.builder()
                .baseUrl(restaurantServiceUrl)
                .build();
    }

    @Bean
    public WebClient userServiceWebClient() {
        return WebClient.builder()
                .baseUrl(userServiceUrl)
                .build();
    }
}
