package ru.vtvhw.spring.finance.dto;

import lombok.Builder;
import lombok.Data;
import ru.vtvhw.spring.finance.enums.TransactionType;

import java.util.UUID;

@Data
@Builder
public class CategoryDto {
    private UUID id;
    private String name;
    private TransactionType type;
    private UUID userId;
}
