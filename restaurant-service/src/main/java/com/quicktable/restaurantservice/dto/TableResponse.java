package com.quicktable.restaurantservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableResponse {

    private Long id;
    private String tableNumber;
    private Integer capacity;
    private String location;
    private Boolean available;
}
