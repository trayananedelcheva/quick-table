package com.quicktable.userservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ForgotPasswordRequest {

    @NotBlank(message = "Имейлът е задължителен")
    @Email(message = "Невалиден формат на имейл")
    private String email;
}
