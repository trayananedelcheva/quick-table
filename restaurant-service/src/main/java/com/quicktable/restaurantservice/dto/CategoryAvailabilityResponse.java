package com.quicktable.restaurantservice.dto;

import com.quicktable.common.dto.TableCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryAvailabilityResponse {
    
    private Long id;
    private Long restaurantId;
    private TableCategory category;
    private Boolean enabled;
}
