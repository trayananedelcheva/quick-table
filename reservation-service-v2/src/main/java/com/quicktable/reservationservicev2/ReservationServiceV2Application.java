package com.quicktable.reservationservicev2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ReservationServiceV2Application {

    public static void main(String[] args) {
        SpringApplication.run(ReservationServiceV2Application.class, args);
    }
}
