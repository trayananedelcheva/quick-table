package com.quicktable.restaurantservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${reservation-service.url}")
    private String reservationServiceUrl;

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }

    @Bean
    public WebClient reservationServiceWebClient(WebClient.Builder builder) {
        return builder.baseUrl(reservationServiceUrl).build();
    }
}
