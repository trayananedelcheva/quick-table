package com.quicktable.reservationservice.dto;

import com.quicktable.common.dto.TableCategory;
import com.quicktable.common.dto.UserRole;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationRequest {

    // User role за валидация (подава се от контролера)
    private UserRole userRole;

    @NotNull(message = "ID на ресторанта е задължително")
    private Long restaurantId;

    // tableId е optional - системата автоматично избира маса според guestsCount
    private Long tableId;

    @NotNull(message = "Датата е задължителна")
    @Future(message = "Датата трябва да е в бъдещето")
    private LocalDate reservationDate;

    @NotNull(message = "Часът е задължителен")
    private LocalTime reservationTime;

    @NotNull(message = "Броят гости е задължителен")
    @Min(value = 1, message = "Броят гости трябва да е поне 1")
    private Integer guestsCount;

    // Предпочитана категория: INSIDE, SUMMER_GARDEN, WINTER_GARDEN (optional)
    private TableCategory preferredCategory; // Optional - ако е null, системата избира произволна категория

    private String specialRequests;

    @NotBlank(message = "Името е задължително")
    private String customerName;

    @NotBlank(message = "Телефонът е задължителен")
    private String customerPhone;

    @Email(message = "Невалиден email")
    private String customerEmail;
}
