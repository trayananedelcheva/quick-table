package com.quicktable.reservationservicev2.scheduler;

import com.quicktable.reservationservicev2.service.ReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class ReservationScheduler {

    private final ReservationService reservationService;

    @Scheduled(cron = "0 */30 * * * *")
    public void autoCompleteExpiredReservations() {
        log.info("Scheduler: проверка за изтекли резервации...");
        int count = reservationService.autoCompleteExpiredReservations();
        if (count > 0) {
            log.info("Scheduler: авто-приключени {} резервации", count);
        }
    }
}
