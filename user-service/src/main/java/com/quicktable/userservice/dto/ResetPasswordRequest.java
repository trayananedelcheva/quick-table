package com.quicktable.userservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordRequest {

    @NotBlank(message = "Токенът е задължителен")
    private String token;

    @NotBlank(message = "Новата парола е задължителна")
    @Size(min = 8, message = "Паролата трябва да е поне 8 символа")
    private String newPassword;
}
