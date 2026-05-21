package com.quicktable.userservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ResetPasswordRequest {

    @NotBlank(message = "Токенът е задължителен")
    private String token;

    @NotBlank(message = "Новата парола е задължителна")
    @Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,}$",
        message = "Паролата трябва да е поне 8 символа и да съдържа поне една буква, една цифра и един специален символ."
    )
    private String newPassword;
}
