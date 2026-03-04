package com.quicktable.restaurantservice.dto;

import com.quicktable.common.dto.TableLocation;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableRequest {

    @NotBlank(message = "Номерът на масата е задължителен")
    private String tableNumber;

    @NotNull(message = "Капацитетът е задължителен")
    @Min(value = 1, message = "Капацитетът трябва да е поне 1")
    private Integer capacity;

    @NotNull(message = "Локацията е задължителна")
    private TableLocation location;
}
