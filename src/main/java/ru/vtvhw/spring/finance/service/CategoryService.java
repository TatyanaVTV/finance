package ru.vtvhw.spring.finance.service;

import ru.vtvhw.spring.finance.dto.CategoryDto;
import ru.vtvhw.spring.finance.entity.Category;
import ru.vtvhw.spring.finance.enums.TransactionType;

import java.util.List;
import java.util.UUID;

public interface CategoryService {
    Category createCategory(String name, TransactionType type, UUID userId);
    List<CategoryDto> getCategoriesByUser(UUID userId);
    List<CategoryDto> getCategoriesByUserAndType(UUID userId, TransactionType type);
    Category getById(UUID id);
    void deleteCategory(UUID id, UUID userId);
}
