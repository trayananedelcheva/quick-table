package com.quicktable.reservationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TimeSlotResponse {
    private String time;
    private boolean available;
}
