package com.quicktable.reservationservicev2.dto;

import com.quicktable.common.dto.TableLocation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableDTO {
    private Long id;
    private String tableNumber;
    private Integer capacity;
    private TableLocation location;
    private Boolean available;
}
