package ru.vtvhw.spring.finance.service;

import ru.vtvhw.spring.finance.dto.category.CategoryResponse;
import ru.vtvhw.spring.finance.entity.Category;
import ru.vtvhw.spring.finance.enums.TransactionType;

import java.util.List;
import java.util.UUID;

public interface CategoryService {
    Category createCategory(String name, TransactionType type, UUID userId);
    Category updateCategory(UUID id, String newName, UUID userId);
    void deleteCategory(UUID id, UUID userId);
    List<CategoryResponse> getCategoriesByUser(UUID userId);
    List<CategoryResponse> getCategoriesByUserAndType(UUID userId, TransactionType type);
    Category getById(UUID id);
}
