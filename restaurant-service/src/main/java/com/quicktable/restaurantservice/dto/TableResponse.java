package com.quicktable.restaurantservice.dto;

import com.quicktable.common.dto.TableLocation;
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
    private TableLocation location;
    private Boolean available;
}
