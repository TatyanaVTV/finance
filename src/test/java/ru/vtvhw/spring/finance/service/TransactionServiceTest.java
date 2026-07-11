package ru.vtvhw.spring.finance.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.vtvhw.spring.finance.dto.TransactionDto;
import ru.vtvhw.spring.finance.entity.Category;
import ru.vtvhw.spring.finance.entity.User;
import ru.vtvhw.spring.finance.enums.TransactionType;
import ru.vtvhw.spring.finance.exception.FinanceSecurityException;
import ru.vtvhw.spring.finance.exception.ResourceNotFoundException;
import ru.vtvhw.spring.finance.exception.ValidationException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static java.math.BigDecimal.TEN;
import static java.math.BigDecimal.ZERO;
import static java.math.RoundingMode.HALF_UP;
import static org.junit.jupiter.api.Assertions.*;
import static ru.vtvhw.spring.finance.enums.TransactionType.EXPENSE;
import static ru.vtvhw.spring.finance.enums.TransactionType.INCOME;

public abstract class TransactionServiceTest {

    protected TransactionService transactionService;

    protected abstract void initService();
    protected abstract User createTestUser(String name, String email, String password);
    protected abstract Category createTestCategory(String name, TransactionType type, User user);
    protected abstract TransactionDto createTestTransactionDto(User user, Category category, BigDecimal amount, TransactionType type, String description);

    @BeforeEach
    public void setUp() {
        initService();
    }

    @Test
    void createTransaction_Income_Success() {
        var user = createTestUser("User", "income@example.com", "pass");
        var category = createTestCategory("Salary", INCOME, user);
        var dto = createTestTransactionDto(user, category, BigDecimal.valueOf(1000), INCOME, "Monthly salary");
        var result = transactionService.createTransaction(dto);
        assertNotNull(result.getId());
        assertEquals(BigDecimal.valueOf(1000), result.getAmount());
        assertEquals(INCOME, result.getType());
        assertEquals("Monthly salary", result.getDescription());
    }

    @Test
    void createTransaction_Expense_WithPositiveAmount_ConvertsToNegative() {
        var user = createTestUser("User", "expense@example.com", "pass");
        var category = createTestCategory("Food", EXPENSE, user);
        var dto = createTestTransactionDto(user, category, BigDecimal.valueOf(500), EXPENSE, "Groceries");
        var result = transactionService.createTransaction(dto);
        assertEquals(BigDecimal.valueOf(-500), result.getAmount());
    }

    @Test
    void createTransaction_Expense_WithNegativeAmount_StaysNegative() {
        var user = createTestUser("User", "expense2@example.com", "pass");
        var category = createTestCategory("Food", EXPENSE, user);
        var dto = createTestTransactionDto(user, category, BigDecimal.valueOf(-300), EXPENSE, "Dinner");
        var result = transactionService.createTransaction(dto);
        assertEquals(BigDecimal.valueOf(-300), result.getAmount());
    }

    @Test
    void createTransaction_UserNotFound_ThrowsException() {
        var unknownUserId = UUID.randomUUID();
        var dto = new TransactionDto();
        dto.setUserId(unknownUserId);
        dto.setAmount(TEN);
        dto.setType(INCOME);
        dto.setDescription("Test");
        assertThrows(ResourceNotFoundException.class,
                () -> transactionService.createTransaction(dto));
    }

    @Test
    void createTransaction_CategoryNotFound_ThrowsException() {
        var user = createTestUser("User", "categoryNotFound@example.com", "pass");
        var dto = createTestTransactionDto(user, null, TEN, INCOME, "Test");
        dto.setCategoryId(UUID.randomUUID()); // несуществующая категория
        assertThrows(ResourceNotFoundException.class,
                () -> transactionService.createTransaction(dto));
    }

    @Test
    void createTransaction_CategoryNotBelongToUser_ThrowsException() {
        var user1 = createTestUser("User1", "user1_transaction@example.com", "pass");
        var user2 = createTestUser("User2", "user2_transaction@example.com", "pass");
        var category = createTestCategory("Shared", INCOME, user1);
        var dto = createTestTransactionDto(user2, category, TEN, INCOME, "Test");
        assertThrows(FinanceSecurityException.class,
                () -> transactionService.createTransaction(dto));
    }

    @Test
    void createTransaction_CategoryTypeMismatch_ThrowsException() {
        var user = createTestUser("User", "mismatch@example.com", "pass");
        var category = createTestCategory("Mismatch", EXPENSE, user); // категория расхода
        var dto = createTestTransactionDto(user, category, TEN, INCOME, "Test");
        assertThrows(ValidationException.class,
                () -> transactionService.createTransaction(dto));
    }

    @Test
    void createTransaction_NegativeIncome_ThrowsException() {
        var user = createTestUser("User", "negIncome@example.com", "pass");
        var category = createTestCategory("Salary", INCOME, user);
        var dto = createTestTransactionDto(user, category, BigDecimal.valueOf(-100), INCOME, "Negative income");
        assertThrows(ValidationException.class,
                () -> transactionService.createTransaction(dto));
    }

    @Test
    void createTransaction_ZeroAmount_ThrowsException() {
        var user = createTestUser("User", "zero@example.com", "pass");
        var category = createTestCategory("Salary", INCOME, user);
        var dto = createTestTransactionDto(user, category, ZERO, INCOME, "Zero income");
        assertThrows(ValidationException.class,
                () -> transactionService.createTransaction(dto));
    }

    @Test
    void createTransaction_EmptyDescription_ThrowsException() {
        var user = createTestUser("User", "emptyDesc@example.com", "pass");
        var category = createTestCategory("Salary", INCOME, user);
        var dto = createTestTransactionDto(user, category, TEN, INCOME, "");
        assertThrows(ValidationException.class,
                () -> transactionService.createTransaction(dto));
    }

    @Test
    void createTransaction_NullDescription_ThrowsException() {
        var user = createTestUser("User", "nullDesc@example.com", "pass");
        var category = createTestCategory("Salary", INCOME, user);
        var dto = createTestTransactionDto(user, category, TEN, INCOME, null);
        assertThrows(ValidationException.class,
                () -> transactionService.createTransaction(dto));
    }

    @Test
    void createTransaction_WithoutCategory_Success() {
        var user = createTestUser("User", "noCategory@example.com", "pass");
        var dto = createTestTransactionDto(user, null, TEN, INCOME, "No category");
        dto.setCategoryId(null);
        var result = transactionService.createTransaction(dto);
        assertNotNull(result.getId());
        assertNull(result.getCategory());
    }

    @Test
    void createTransaction_WithoutDate_ShouldSetCurrentDate() {
        var user = createTestUser("User", "noDate@example.com", "pass");
        var category = createTestCategory("Salary", INCOME, user);
        var dto = createTestTransactionDto(user, category, TEN, INCOME, "No date");
        dto.setDate(null); // явно убираем дату
        var result = transactionService.createTransaction(dto);
        assertNotNull(result.getDate());
        assertTrue(result.getDate().isAfter(LocalDateTime.now().minusSeconds(5)));
    }

    @Test
    void updateTransaction_WithoutDate_ShouldNotChangeDate() {
        var user = createTestUser("User", "updateNoDate@example.com", "pass");
        var category = createTestCategory("Salary", INCOME, user);
        var originalDate = LocalDateTime.of(2025, 1, 1, 10, 0);
        var dto = createTestTransactionDto(user, category, TEN, INCOME, "Old");
        dto.setDate(originalDate);
        var created = transactionService.createTransaction(dto);

        var updateDto = new TransactionDto();
        updateDto.setUserId(user.getId());
        updateDto.setAmount(BigDecimal.valueOf(20));
        updateDto.setType(INCOME);
        updateDto.setCategoryId(category.getId());
        updateDto.setDescription("Updated");
        updateDto.setDate(null);

        var updated = transactionService.updateTransaction(created.getId(), updateDto);
        assertEquals(originalDate, updated.getDate());
    }

    @Test
    void getTransactionsForUser_WithNullFrom_ShouldWork() {
        var user = createTestUser("User", "nullFrom@example.com", "pass");
        var category = createTestCategory("Salary-" + System.currentTimeMillis(), INCOME, user);

        var dto1 = createTestTransactionDto(user, category, TEN, INCOME, "Jan");
        dto1.setDate(LocalDateTime.of(2026, 1, 15, 10, 0));
        transactionService.createTransaction(dto1);

        var dto2 = createTestTransactionDto(user, category, BigDecimal.valueOf(20), INCOME, "Feb");
        dto2.setDate(LocalDateTime.of(2026, 2, 15, 10, 0));
        transactionService.createTransaction(dto2);

        var to = LocalDateTime.of(2026, 2, 1, 0, 0);
        var result = transactionService.getTransactionsForUser(user.getId(), null, to);

        assertEquals(1, result.size());
        assertEquals("Jan", result.getFirst().getDescription());
    }

    @Test
    void getTransactionsForUser_WithNullTo_ShouldWork() {
        var user = createTestUser("User", "nullTo@example.com", "pass");
        var category = createTestCategory("Salary-" + System.currentTimeMillis(), INCOME, user);

        var dto1 = createTestTransactionDto(user, category, TEN, INCOME, "Jan");
        dto1.setDate(LocalDateTime.of(2026, 1, 15, 10, 0));
        transactionService.createTransaction(dto1);

        var dto2 = createTestTransactionDto(user, category, BigDecimal.valueOf(20), INCOME, "Feb");
        dto2.setDate(LocalDateTime.of(2026, 2, 15, 10, 0));
        transactionService.createTransaction(dto2);

        var from = LocalDateTime.of(2026, 2, 1, 0, 0);
        var result = transactionService.getTransactionsForUser(user.getId(), from, null);

        assertEquals(1, result.size());
        assertEquals("Feb", result.getFirst().getDescription());
    }

    @Test
    void getTransactionsForUser_WithNullBoth_ShouldReturnAll() {
        var user = createTestUser("User", "nullBoth@example.com", "pass");
        var category = createTestCategory("Salary-" + System.currentTimeMillis(), INCOME, user);

        var dto1 = createTestTransactionDto(user, category, TEN, INCOME, "First");
        var dto2 = createTestTransactionDto(user, category, BigDecimal.valueOf(20), INCOME, "Second");

        transactionService.createTransaction(dto1);
        transactionService.createTransaction(dto2);
        var result = transactionService.getTransactionsForUser(user.getId(), null, null);
        assertEquals(2, result.size());
    }

    @Test
    void getTotalAmount_WhenNoTransactions_ReturnsZero() {
        var user = createTestUser("User", "zeroTotal@example.com", "pass");

        var from = LocalDateTime.now().minusDays(1);
        var to = LocalDateTime.now().plusDays(1);
        var total = transactionService.getTotalAmount(user.getId(), INCOME, from, to);
        assertEquals(ZERO, total);
    }

    @Test
    void updateTransaction_Success() {
        var user = createTestUser("User", "update@example.com", "pass");
        var category = createTestCategory("Salary", INCOME, user);
        var dto = createTestTransactionDto(user, category, BigDecimal.valueOf(1000), INCOME, "Old desc");
        var created = transactionService.createTransaction(dto);

        var updateDto = new TransactionDto();
        updateDto.setUserId(user.getId());
        updateDto.setAmount(BigDecimal.valueOf(2000));
        updateDto.setType(INCOME);
        updateDto.setCategoryId(category.getId());
        updateDto.setDate(LocalDateTime.now());
        updateDto.setDescription("New desc");

        var updated = transactionService.updateTransaction(created.getId(), updateDto);
        assertEquals(BigDecimal.valueOf(2000), updated.getAmount());
        assertEquals("New desc", updated.getDescription());
    }

    @Test
    void updateTransaction_TransactionNotFound_ThrowsException() {
        var user = createTestUser("User", "notFound@example.com", "pass");
        var dto = new TransactionDto();
        dto.setUserId(user.getId());
        dto.setAmount(TEN);
        dto.setType(INCOME);
        dto.setDescription("Test");
        assertThrows(ResourceNotFoundException.class,
                () -> transactionService.updateTransaction(UUID.randomUUID(), dto));
    }

    @Test
    void updateTransaction_NotBelongToUser_ThrowsException() {
        var user1 = createTestUser("User1", "user1upd@example.com", "pass");
        var user2 = createTestUser("User2", "user2upd@example.com", "pass");
        var category = createTestCategory("Salary", INCOME, user1);
        var dto = createTestTransactionDto(user1, category, TEN, INCOME, "Desc");
        var created = transactionService.createTransaction(dto);

        var updateDto = new TransactionDto();
        updateDto.setUserId(user2.getId());
        updateDto.setAmount(BigDecimal.valueOf(20));
        updateDto.setType(INCOME);
        updateDto.setDescription("New desc");

        assertThrows(FinanceSecurityException.class,
                () -> transactionService.updateTransaction(created.getId(), updateDto));
    }

    @Test
    void updateTransaction_CategoryTypeMismatch_ThrowsException() {
        var user = createTestUser("User", "updMismatch@example.com", "pass");
        var categoryIncome = createTestCategory("Income", INCOME, user);
        var categoryExpense = createTestCategory("Expense", EXPENSE, user);
        var dto = createTestTransactionDto(user, categoryIncome, TEN, INCOME, "Old");
        var created = transactionService.createTransaction(dto);

        var updateDto = new TransactionDto();
        updateDto.setUserId(user.getId());
        updateDto.setAmount(TEN);
        updateDto.setType(INCOME);
        updateDto.setCategoryId(categoryExpense.getId()); // mismatch
        updateDto.setDescription("New desc");

        assertThrows(ValidationException.class,
                () -> transactionService.updateTransaction(created.getId(), updateDto));
    }

    @Test
    void deleteTransaction_Success() {
        var user = createTestUser("User", "delete@example.com", "pass");
        var category = createTestCategory("Salary", INCOME, user);
        var dto = createTestTransactionDto(user, category, TEN, INCOME, "To delete");
        var created = transactionService.createTransaction(dto);
        assertDoesNotThrow(() -> transactionService.deleteTransaction(created.getId(), user.getId()));
    }

    @Test
    void deleteTransaction_NotFound_ThrowsException() {
        var user = createTestUser("User", "deleteNotFound@example.com", "pass");
        assertThrows(ResourceNotFoundException.class,
                () -> transactionService.deleteTransaction(UUID.randomUUID(), user.getId()));
    }

    @Test
    void deleteTransaction_NotBelongToUser_ThrowsException() {
        var user1 = createTestUser("User1", "delUser1@example.com", "pass");
        var user2 = createTestUser("User2", "delUser2@example.com", "pass");
        var category = createTestCategory("Salary", INCOME, user1);
        var dto = createTestTransactionDto(user1, category, TEN, INCOME, "Old");
        var created = transactionService.createTransaction(dto);
        assertThrows(FinanceSecurityException.class,
                () -> transactionService.deleteTransaction(created.getId(), user2.getId()));
    }

    @Test
    void getTransactionsForUser_WithDateFilter_ReturnsFiltered() {
        var user = createTestUser("User", "filter@example.com", "pass");
        var category = createTestCategory("Salary", INCOME, user);
        var dto1 = createTestTransactionDto(user, category, TEN, INCOME, "Jan");
        dto1.setDate(LocalDateTime.of(2026, 1, 15, 10, 0));
        transactionService.createTransaction(dto1);
        var dto2 = createTestTransactionDto(user, category, BigDecimal.valueOf(20), INCOME, "Feb");
        dto2.setDate(LocalDateTime.of(2026, 2, 15, 10, 0));
        transactionService.createTransaction(dto2);

        var from = LocalDateTime.of(2026, 2, 1, 0, 0);
        var to = LocalDateTime.of(2026, 2, 28, 23, 59);
        var result = transactionService.getTransactionsForUser(user.getId(), from, to);
        assertEquals(1, result.size());
        assertEquals("Feb", result.getFirst().getDescription());
    }

    @Test
    void getTransactionsForUser_InvalidDateRange_ThrowsException() {
        var user = createTestUser("User", "invalidDate@example.com", "pass");
        var from = LocalDateTime.of(2026, 2, 1, 0, 0);
        var to = LocalDateTime.of(2026, 1, 31, 23, 59);
        assertThrows(ValidationException.class,
                () -> transactionService.getTransactionsForUser(user.getId(), from, to));
    }

    @Test
    void getTransactionById_Success() {
        var user = createTestUser("User", "getById@example.com", "pass");
        var category = createTestCategory("Salary", INCOME, user);
        var dto = createTestTransactionDto(user, category, TEN, INCOME, "Test");
        var created = transactionService.createTransaction(dto);
        var found = transactionService.getTransactionById(created.getId(), user.getId());
        assertEquals(created.getId(), found.getId());
        assertEquals("Test", found.getDescription());
    }

    @Test
    void getTransactionById_NotFound_ThrowsException() {
        var user = createTestUser("User", "getByIdNotFound@example.com", "pass");
        assertThrows(ResourceNotFoundException.class,
                () -> transactionService.getTransactionById(UUID.randomUUID(), user.getId()));
    }

    @Test
    void getTransactionById_NotBelongToUser_ThrowsException() {
        var user1 = createTestUser("User1", "getByIdUser1@example.com", "pass");
        var user2 = createTestUser("User2", "getByIdUser2@example.com", "pass");
        var category = createTestCategory("Salary", INCOME, user1);
        var dto = createTestTransactionDto(user1, category, TEN, INCOME, "Old");
        var created = transactionService.createTransaction(dto);
        assertThrows(FinanceSecurityException.class,
                () -> transactionService.getTransactionById(created.getId(), user2.getId()));
    }

    @Test
    void getTotalAmount_ReturnsCorrectSum() {
        var user = createTestUser("User", "total@example.com", "pass");
        var catIncome = createTestCategory("Salary", INCOME, user);
        var catExpense = createTestCategory("Food", EXPENSE, user);
        transactionService.createTransaction(createTestTransactionDto(user, catIncome, BigDecimal.valueOf(1000), INCOME, "Sal"));
        transactionService.createTransaction(createTestTransactionDto(user, catExpense, BigDecimal.valueOf(-500), EXPENSE, "Groceries"));

        var from = LocalDateTime.now().minusDays(1);
        var to = LocalDateTime.now().plusDays(1);
        var totalIncome = transactionService.getTotalAmount(user.getId(), INCOME, from, to);
        var totalExpense = transactionService.getTotalAmount(user.getId(), EXPENSE, from, to);
        assertEquals(BigDecimal.valueOf(1000).setScale(2, HALF_UP), totalIncome);
        assertEquals(BigDecimal.valueOf(-500).setScale(2, HALF_UP), totalExpense);
    }

    @Test
    void getAllTransactionsForUser_ReturnsAll() {
        var user = createTestUser("User", "all@example.com", "pass");
        var category = createTestCategory("Salary", INCOME, user);
        transactionService.createTransaction(createTestTransactionDto(user, category, TEN, INCOME, "First"));
        transactionService.createTransaction(createTestTransactionDto(user, category, BigDecimal.valueOf(20), INCOME, "Second"));
        var result = transactionService.getAllTransactionsForUser(user.getId());
        assertEquals(2, result.size());
    }

    @Test
    void getAllTransactionsForUser_EmptyList_ReturnsEmpty() {
        var user = createTestUser("User", "empty@example.com", "pass");
        var result = transactionService.getAllTransactionsForUser(user.getId());
        assertTrue(result.isEmpty());
    }
}
