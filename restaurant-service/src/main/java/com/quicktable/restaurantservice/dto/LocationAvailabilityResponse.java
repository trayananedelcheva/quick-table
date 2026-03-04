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
public class LocationAvailabilityResponse {
    
    private Long id;
    private Long restaurantId;
    private TableLocation location;
    private Boolean enabled;
}
