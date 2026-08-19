package ru.vtvhw.spring.finance.dto;

import lombok.Data;
import ru.vtvhw.spring.finance.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class TransactionDto {
    private UUID id;
    private UUID userId;
    private BigDecimal amount;
    private TransactionType type;
    private UUID categoryId;
    private String categoryName;
    private LocalDateTime date;
    private String description;
}
