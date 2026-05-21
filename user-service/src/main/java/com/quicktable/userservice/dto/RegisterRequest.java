package com.quicktable.userservice.dto;

import com.quicktable.common.dto.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Email е задължителен.")
    @Email(message = "Невалиден email формат.")
    private String email;

    @NotBlank(message = "Паролата е задължителна.")
    @Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,}$",
        message = "Паролата трябва да е поне 8 символа и да съдържа поне една буква, една цифра и един специален символ."
    )
    private String password;

    @NotBlank(message = "Първото име е задължително.")
    private String firstName;

    @NotBlank(message = "Фамилията е задължителна.")
    private String lastName;

    private String phoneNumber;

    // NOTE: Role се игнорира при регистрация - всички са CLIENT
    // Само SYSTEM_ADMIN може да променя роли след това
}
