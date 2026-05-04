package com.quicktable.restaurantservice.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewRequest {

    @NotNull(message = "ID на резервацията е задължително")
    private Long reservationId;

    @NotNull(message = "Оценката е задължителна")
    @Min(value = 1, message = "Оценката трябва да е между 1 и 5")
    @Max(value = 5, message = "Оценката трябва да е между 1 и 5")
    private Integer rating;

    private String comment;
}
