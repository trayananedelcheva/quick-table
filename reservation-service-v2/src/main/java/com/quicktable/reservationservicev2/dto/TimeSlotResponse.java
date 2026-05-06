package com.quicktable.reservationservicev2.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalTime;

@Getter
@AllArgsConstructor
public class TimeSlotResponse {
    private LocalTime time;
    private boolean available;
}
