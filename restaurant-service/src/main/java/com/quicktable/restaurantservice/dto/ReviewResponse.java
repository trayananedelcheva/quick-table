package com.quicktable.restaurantservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {

    private Long id;
    private Long restaurantId;
    private Long userId;
    private Long reservationId;
    private String customerName;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
}
