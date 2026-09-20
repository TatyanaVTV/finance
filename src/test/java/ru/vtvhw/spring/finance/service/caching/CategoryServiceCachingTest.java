package ru.vtvhw.spring.finance.service.caching;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;
import ru.vtvhw.spring.finance.entity.User;
import ru.vtvhw.spring.finance.repository.CategoryRepository;
import ru.vtvhw.spring.finance.repository.TransactionRepository;
import ru.vtvhw.spring.finance.service.CategoryService;
import ru.vtvhw.spring.finance.service.UserService;

import java.util.Objects;
import java.util.UUID;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static ru.vtvhw.spring.finance.enums.TransactionType.EXPENSE;
import static ru.vtvhw.spring.finance.enums.TransactionType.INCOME;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CategoryServiceCachingTest {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private UserService userService;

    @Autowired
    private CacheManager cacheManager;

    @MockitoSpyBean
    private CategoryRepository categoryRepository;

    @MockitoSpyBean
    private TransactionRepository transactionRepository;

    private User user;
    private UUID userId;

    @BeforeEach
    void setUp() {
        cacheManager.getCacheNames()
                .forEach(name -> Objects.requireNonNull(cacheManager.getCache(name)).clear());

        var email = format("cache-cat-%s@example.com", System.currentTimeMillis());
        user = userService.createUser("Cache User", email, "password");
        userId = user.getId();
    }

    @Test
    void getCategoriesByUser_secondCall_usesCache() {
        categoryService.createCategory("Cached1", INCOME, userId);
        categoryService.createCategory("Cached2", EXPENSE, userId);
        clearInvocations(categoryRepository);

        var first = categoryService.getCategoriesByUser(userId);
        var second = categoryService.getCategoriesByUser(userId);

        assertThat(first).hasSize(2);
        assertThat(second).hasSize(2);
        verify(categoryRepository, times(1)).findByUserId(userId);
    }

    @Test
    void getCategoriesByUser_differentUsers_doNotShareCache() {
        var otherEmail = format("cache-cat-other-%s@example.com", System.currentTimeMillis());
        var otherUser = userService.createUser("Other", otherEmail, "password");

        categoryService.createCategory("Mine", INCOME, userId);
        categoryService.createCategory("Theirs", INCOME, otherUser.getId());
        clearInvocations(categoryRepository);

        categoryService.getCategoriesByUser(userId);
        categoryService.getCategoriesByUser(otherUser.getId());

        verify(categoryRepository, times(1)).findByUserId(userId);
        verify(categoryRepository, times(1)).findByUserId(otherUser.getId());
    }

    @Test
    void getCategoriesByUserAndType_secondCall_usesCache() {
        categoryService.createCategory("Income", INCOME, userId);
        categoryService.createCategory("Expense", EXPENSE, userId);
        clearInvocations(categoryRepository);

        categoryService.getCategoriesByUserAndType(userId, INCOME);
        categoryService.getCategoriesByUserAndType(userId, INCOME);

        verify(categoryRepository, times(1)).findByUserIdAndType(userId, INCOME);
    }

    @Test
    void getCategoriesByUserAndType_differentTypes_doNotShareCache() {
        categoryService.createCategory("Income", INCOME, userId);
        categoryService.createCategory("Expense", EXPENSE, userId);
        clearInvocations(categoryRepository);

        categoryService.getCategoriesByUserAndType(userId, INCOME);
        categoryService.getCategoriesByUserAndType(userId, EXPENSE);

        verify(categoryRepository, times(1)).findByUserIdAndType(userId, INCOME);
        verify(categoryRepository, times(1)).findByUserIdAndType(userId, EXPENSE);
    }

    @Test
    void createCategory_evictsCache() {
        categoryService.getCategoriesByUser(userId); // заполнить кэш
        clearInvocations(categoryRepository);

        categoryService.createCategory("New", INCOME, userId); // evict

        categoryService.getCategoriesByUser(userId); // должен сходить в БД
        verify(categoryRepository, times(1)).findByUserId(userId);
    }

    @Test
    void updateCategory_evictsCache() {
        var created = categoryService.createCategory("Old", INCOME, userId);
        categoryService.getCategoriesByUser(userId); // заполнить кэш
        clearInvocations(categoryRepository);

        categoryService.updateCategory(created.getId(), "New", userId); // evict

        categoryService.getCategoriesByUser(userId);
        verify(categoryRepository, times(1)).findByUserId(userId);
    }

    @Test
    void deleteCategory_evictsCache() {
        var created = categoryService.createCategory("ToDelete", INCOME, userId);
        categoryService.getCategoriesByUser(userId); // заполнить кэш
        clearInvocations(categoryRepository);

        categoryService.deleteCategory(created.getId(), userId); // evict

        categoryService.getCategoriesByUser(userId);
        verify(categoryRepository, times(1)).findByUserId(userId);
    }

    @Test
    void createCategory_evictsByTypeCache() {
        categoryService.getCategoriesByUserAndType(userId, INCOME); // заполнить кэш
        clearInvocations(categoryRepository);

        categoryService.createCategory("NewIncome", INCOME, userId); // evict

        categoryService.getCategoriesByUserAndType(userId, INCOME);
        verify(categoryRepository, times(1)).findByUserIdAndType(userId, INCOME);
    }
}
