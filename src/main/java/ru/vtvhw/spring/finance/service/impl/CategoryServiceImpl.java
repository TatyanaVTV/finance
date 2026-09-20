package ru.vtvhw.spring.finance.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.vtvhw.spring.finance.dto.category.CategoryResponse;
import ru.vtvhw.spring.finance.entity.Category;
import ru.vtvhw.spring.finance.enums.TransactionType;
import ru.vtvhw.spring.finance.mapper.CategoryMapper;
import ru.vtvhw.spring.finance.repository.CategoryRepository;
import ru.vtvhw.spring.finance.repository.TransactionRepository;
import ru.vtvhw.spring.finance.repository.UserRepository;
import ru.vtvhw.spring.finance.service.CategoryService;

import java.util.List;
import java.util.UUID;

import static org.apache.logging.log4j.util.Strings.isBlank;
import static ru.vtvhw.spring.finance.exception.FinanceSecurityException.categoryNotBelongToUser;
import static ru.vtvhw.spring.finance.exception.ResourceNotFoundException.categoryNotFound;
import static ru.vtvhw.spring.finance.exception.ResourceNotFoundException.userNotFound;
import static ru.vtvhw.spring.finance.exception.ValidationException.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final CategoryMapper categoryMapper;

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "categoriesByUser", key = "#userId"),
            @CacheEvict(value = "categoriesByUserAndType", allEntries = true)
    })
    public Category createCategory(String name, TransactionType type, UUID userId) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User not found for category creation: {}", userId);
                    return userNotFound(userId);
                });
        if (isBlank(name)) {
            throw emptyCategoryName();
        }

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
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "categoriesByUser", key = "#userId"),
            @CacheEvict(value = "categoriesByUserAndType", allEntries = true)
    })
    public Category updateCategory(UUID id, String newName, UUID userId) {
        var category = getById(id);
        if (!category.getUser().getId().equals(userId)) {
            throw categoryNotBelongToUser(id, userId);
        }
        if (isBlank(newName)) {
            throw emptyCategoryName();
        }
        // Проверка уникальности для этого пользователя
        if (categoryRepository.existsByNameAndUserIdAndIdNot(newName, userId, id)) {
            throw categoryAlreadyExists(newName);
        }
        category.setName(newName);
        var updated = categoryRepository.save(category);
        log.info("Category updated: id={}, newName={}", updated.getId(), updated.getName());
        return updated;
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "categoriesByUser", key = "#userId"),
            @CacheEvict(value = "categoriesByUserAndType", allEntries = true)
    })
    public void deleteCategory(UUID categoryId, UUID userId) {
        var category = getById(categoryId);
        if (!category.getUser().getId().equals(userId)) {
            throw categoryNotBelongToUser(categoryId, userId);
        }

        if (transactionRepository.existsByCategoryId(categoryId)) {
            throw categoryHasTransactions(categoryId);
        }

        categoryRepository.delete(category);
        log.info("Category {} deleted for user {}", categoryId, userId);
    }

    @Override
    @Cacheable(value = "categoriesByUser", key = "#userId")
    public List<CategoryResponse> getCategoriesByUser(UUID userId) {
        return categoryRepository.findByUserId(userId).stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    @Override
    @Cacheable(value = "categoriesByUserAndType", key = "#userId + ':' + #type")
    public List<CategoryResponse> getCategoriesByUserAndType(UUID userId, TransactionType type) {
        return categoryRepository.findByUserIdAndType(userId, type).stream()
                .map(categoryMapper::toResponse)
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
}
