package com.quicktable.reservationservice.dto;

import com.quicktable.common.dto.ReservationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateReservationStatusRequest {

    @NotNull(message = "Статусът е задължителен")
    private ReservationStatus status;
    
    private String note;
}
