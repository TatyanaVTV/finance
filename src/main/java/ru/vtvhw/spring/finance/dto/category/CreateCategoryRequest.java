package ru.vtvhw.spring.finance.dto.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import ru.vtvhw.spring.finance.enums.TransactionType;

@Data
public class CreateCategoryRequest {

    @NotBlank(message = "Название категории обязательно")
    @Size(max = 50, message = "Название не должно превышать 50 символов")
    private String name;

    @NotNull(message = "Тип транзакции обязателен")
    private TransactionType type;
}
