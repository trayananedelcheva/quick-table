package com.quicktable.userservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "Email е задължителен")
    @Email(message = "Невалиден email формат")
    private String email;

    @NotBlank(message = "Паролата е задължителна")
    private String password;
}
