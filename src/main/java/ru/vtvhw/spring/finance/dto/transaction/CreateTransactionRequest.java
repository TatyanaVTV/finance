package ru.vtvhw.spring.finance.dto.transaction;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import ru.vtvhw.spring.finance.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class CreateTransactionRequest {

    @NotNull(message = "Сумма обязательна")
    @Digits(integer = 17, fraction = 2,
            message = "Сумма должна иметь не более 17 цифр до запятой и 2 после")
    private BigDecimal amount;

    @NotNull(message = "Тип транзакции обязателен")
    private TransactionType type;

    private UUID categoryId;
    private LocalDateTime date;

    @NotBlank(message = "Описание обязательно")
    @Size(max = 255, message = "Описание не должно превышать 255 символов")
    private String description;
}
