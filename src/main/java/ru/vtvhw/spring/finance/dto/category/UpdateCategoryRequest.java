package ru.vtvhw.spring.finance.dto.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateCategoryRequest {

    @NotBlank(message = "Новое название обязательно")
    @Size(max = 50, message = "Новое название должно содержать не более 50 символов")
    private String name;
}
