package com.quicktable.reservationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO за маса, получена от restaurant-service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableDTO {
    private Long id;
    private String tableNumber;
    private Integer capacity;
    private String location; // "Вътре", "Тераса", "Градина", etc.
    private Boolean available;
}
