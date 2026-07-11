package ru.vtvhw.spring.finance.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.vtvhw.spring.finance.dto.CategoryDto;
import ru.vtvhw.spring.finance.entity.Category;
import ru.vtvhw.spring.finance.enums.TransactionType;
import ru.vtvhw.spring.finance.mapper.CategoryMapper;
import ru.vtvhw.spring.finance.repository.CategoryRepository;
import ru.vtvhw.spring.finance.repository.UserRepository;
import ru.vtvhw.spring.finance.service.CategoryService;

import java.util.List;
import java.util.UUID;

import static ru.vtvhw.spring.finance.exception.FinanceSecurityException.categoryNotBelongToUser;
import static ru.vtvhw.spring.finance.exception.ResourceNotFoundException.categoryNotFound;
import static ru.vtvhw.spring.finance.exception.ResourceNotFoundException.userNotFound;
import static ru.vtvhw.spring.finance.exception.ValidationException.categoryAlreadyExists;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final CategoryMapper categoryMapper;

    @Override
    @Transactional
    public Category createCategory(String name, TransactionType type, UUID userId) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User not found for category creation: {}", userId);
                    return userNotFound(userId);
                });

        if (categoryRepository.existsByNameAndUserId(name, userId)) {
            throw categoryAlreadyExists(name);
        }

        var category = Category.builder()
                .name(name)
                .type(type)
                .user(user)
                .build();
        var saved = categoryRepository.save(category);
        log.info("Created category: {} for user {}", name, userId);
        return saved;
    }

    @Override
    public List<CategoryDto> getCategoriesByUser(UUID userId) {
        return categoryRepository.findByUserId(userId).stream()
                .map(categoryMapper::toDto)
                .toList();
    }

    @Override
    public List<CategoryDto> getCategoriesByUserAndType(UUID userId, TransactionType type) {
        return categoryRepository.findByUserIdAndType(userId, type).stream()
                .map(categoryMapper::toDto)
                .toList();
    }

    @Override
    public Category getById(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Category not found: {}", id);
                    return categoryNotFound(id);
                });
    }

    @Override
    @Transactional
    public void deleteCategory(UUID categoryId, UUID userId) {
        var category = getById(categoryId);
        if (!category.getUser().getId().equals(userId)) {
            throw categoryNotBelongToUser(categoryId, userId);
        }
        categoryRepository.delete(category);
        log.info("Category {} deleted for user {}", categoryId, userId);
    }
}
