package com.quicktable.restaurantservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationGroupResponse {
    
    private String displayName; // "Вътре", "Лятна градина", "Зимна градина"
    private Boolean enabled;    // Дали локацията е активна
    private List<TableResponse> tables; // Маси в тази локация
}
