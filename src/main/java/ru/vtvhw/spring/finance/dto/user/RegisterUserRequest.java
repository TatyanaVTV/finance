package ru.vtvhw.spring.finance.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterUserRequest {

    @NotBlank(message = "Имя обязательно")
    @Size(max = 100, message = "Имя не должно превышать 100 символов")
    private String name;

    @NotBlank(message = "Email обязателен")
    @Email(message = "Некорректный email")
    @Size(max = 150, message = "Email не должен превышать 150 символов")
    private String email;

    // BCrypt обрезает пароль на 72 байтах. Ограничиваем 64 символами с запасом.
    @NotBlank(message = "Пароль обязателен")
    @Size(min = 6, max = 64, message = "Пароль должен содержать от 6 до 64 символов")
    private String password;
}
