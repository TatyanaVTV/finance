package ru.vtvhw.spring.finance.dto.category;

import lombok.Builder;
import lombok.Data;
import ru.vtvhw.spring.finance.enums.TransactionType;

import java.util.UUID;

@Data
@Builder
public class CategoryResponse {
    private UUID id;
    private String name;
    private TransactionType type;
    private UUID userId;
}
