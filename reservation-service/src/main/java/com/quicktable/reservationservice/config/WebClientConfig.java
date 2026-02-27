package com.quicktable.reservationservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }

    @Bean
    public WebClient restaurantServiceWebClient(WebClient.Builder builder) {
        return builder
                .baseUrl("http://localhost:8082") // Restaurant Service URL
                .build();
    }
}
