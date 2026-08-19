package ru.vtvhw.spring.finance.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.vtvhw.spring.finance.entity.Category;
import ru.vtvhw.spring.finance.entity.User;
import ru.vtvhw.spring.finance.enums.TransactionType;
import ru.vtvhw.spring.finance.exception.ResourceNotFoundException;
import ru.vtvhw.spring.finance.exception.ValidationException;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static ru.vtvhw.spring.finance.enums.TransactionType.INCOME;

public abstract class CategoryServiceTest {

    protected CategoryService categoryService;

    protected User user;
    protected UUID userId;
    protected Category category;
    protected UUID categoryId;

    protected abstract void initService();
    protected abstract void createTransactionForCategory(UUID userId, UUID categoryId, TransactionType type);
    protected abstract User createTestUser();

    @BeforeEach
    void setUp() {
        user = createTestUser();
        userId = user.getId();
        categoryId = UUID.randomUUID();
        user = User.builder().id(userId).name("Test User").build();
        category = Category.builder()
                .id(categoryId)
                .name("Test Category")
                .type(INCOME)
                .user(user)
                .build();

        initService();
    }

    @Test
    void createCategory_Success() {
        var result = categoryService.createCategory("New Category", INCOME, userId);
        assertNotNull(result);
        assertEquals("New Category", result.getName());
        assertNotNull(result.getId());
    }

    @Test
    void createCategory_DuplicateName_ThrowsException() {
        categoryService.createCategory("Existing", INCOME, userId);
        assertThrows(ValidationException.class,
                () -> categoryService.createCategory("Existing", INCOME, userId));
    }

    @Test
    void createCategory_UserNotFound_ThrowsException() {
        var unknownUserId = UUID.randomUUID();
        assertThrows(ResourceNotFoundException.class,
                () -> categoryService.createCategory("New", INCOME, unknownUserId));
    }

    @Test
    void updateCategory_Success() {
        var created = categoryService.createCategory("Old Name", INCOME, userId);
        var updated = categoryService.updateCategory(created.getId(), "New Name", userId);
        assertEquals("New Name", updated.getName());
    }

    @Test
    void updateCategory_EmptyName_ThrowsException() {
        var created = categoryService.createCategory("Old Name", INCOME, userId);
        assertThrows(ValidationException.class,
                () -> categoryService.updateCategory(created.getId(), "", userId));
    }

    @Test
    void updateCategory_DuplicateName_ThrowsException() {
        var cat1 = categoryService.createCategory("Cat1", INCOME, userId);
        categoryService.createCategory("Cat2", INCOME, userId);
        assertThrows(ValidationException.class,
                () -> categoryService.updateCategory(cat1.getId(), "Cat2", userId));
    }

    @Test
    void updateCategory_NotBelongToUser_ThrowsException() {
        var otherUserId = UUID.randomUUID();
        var cat = categoryService.createCategory("Old", INCOME, userId);
        assertThrows(SecurityException.class,
                () -> categoryService.updateCategory(cat.getId(), "New", otherUserId));
    }

    @Test
    void deleteCategory_Success() {
        var cat = categoryService.createCategory("ToDelete", INCOME, userId);
        assertDoesNotThrow(() -> categoryService.deleteCategory(cat.getId(), userId));
        // Проверка, что категория удалена (можно через getById или другие методы)
    }

    @Test
    void deleteCategory_WithTransactions_ThrowsException() {
        var cat = categoryService.createCategory("ToDelete", INCOME, userId);
        createTransactionForCategory(userId, cat.getId(), INCOME);
        assertThrows(ValidationException.class,
                () -> categoryService.deleteCategory(cat.getId(), userId));
    }

    @Test
    void deleteCategory_NotBelongToUser_ThrowsException() {
        var cat = categoryService.createCategory("ToDelete", INCOME, userId);
        var otherUserId = UUID.randomUUID();
        assertThrows(SecurityException.class,
                () -> categoryService.deleteCategory(cat.getId(), otherUserId));
    }

    @Test
    void getCategoriesByUser_ReturnsList() {
        categoryService.createCategory("Cat1", INCOME, userId);
        categoryService.createCategory("Cat2", TransactionType.EXPENSE, userId);
        var list = categoryService.getCategoriesByUser(userId);
        assertEquals(2, list.size());
    }

    @Test
    void getCategoriesByUser_EmptyList() {
        var list = categoryService.getCategoriesByUser(userId);
        assertTrue(list.isEmpty());
    }

    @Test
    void getCategoriesByUserAndType_FiltersCorrectly() {
        categoryService.createCategory("Income1", INCOME, userId);
        categoryService.createCategory("Expense1", TransactionType.EXPENSE, userId);
        var incomeList = categoryService.getCategoriesByUserAndType(userId, INCOME);
        assertEquals(1, incomeList.size());
        assertEquals("Income1", incomeList.getFirst().getName());
    }

    @Test
    void getById_Success() {
        var created = categoryService.createCategory("GetMe", INCOME, userId);
        var found = categoryService.getById(created.getId());
        assertEquals(created.getName(), found.getName());
    }

    @Test
    void getById_NotFound_ThrowsException() {
        assertThrows(ResourceNotFoundException.class,
                () -> categoryService.getById(UUID.randomUUID()));
    }
}
