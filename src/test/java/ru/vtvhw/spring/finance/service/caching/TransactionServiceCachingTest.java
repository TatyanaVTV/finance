package ru.vtvhw.spring.finance.service.caching;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;
import ru.vtvhw.spring.finance.dto.transaction.TransactionDto;
import ru.vtvhw.spring.finance.entity.Category;
import ru.vtvhw.spring.finance.entity.User;
import ru.vtvhw.spring.finance.repository.TransactionRepository;
import ru.vtvhw.spring.finance.service.CategoryService;
import ru.vtvhw.spring.finance.service.TransactionService;
import ru.vtvhw.spring.finance.service.UserService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static java.lang.String.format;
import static org.mockito.Mockito.*;
import static ru.vtvhw.spring.finance.enums.TransactionType.EXPENSE;
import static ru.vtvhw.spring.finance.enums.TransactionType.INCOME;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TransactionServiceCachingTest {

    @Autowired
    private TransactionService transactionService;
    @Autowired private UserService userService;
    @Autowired private CategoryService categoryService;
    @Autowired private CacheManager cacheManager;

    @MockitoSpyBean private TransactionRepository transactionRepository;

    private User user;
    private UUID userId;
    private Category category;
    private LocalDateTime from;
    private LocalDateTime to;

    @BeforeEach
    void setUp() {
        cacheManager.getCacheNames()
                .forEach(name -> cacheManager.getCache(name).clear());

        var email = format("cache-tx-%s@example.com", System.currentTimeMillis());
        user = userService.createUser("Cache User", email, "password");
        userId = user.getId();
        category = categoryService.createCategory("Cache", INCOME, userId);

        from = LocalDateTime.now().minusDays(30);
        to = LocalDateTime.now().plusDays(1);
    }

    @Test
    void getTotalAmount_secondCall_usesCache() {
        transactionService.getTotalAmount(userId, INCOME, from, to);
        transactionService.getTotalAmount(userId, INCOME, from, to);

        verify(transactionRepository, times(1))
                .sumAmountByUserAndTypeAndDateRange(userId, INCOME, from, to);
    }

    @Test
    void getTotalAmount_differentTypes_doNotShareCache() {
        transactionService.getTotalAmount(userId, INCOME, from, to);
        transactionService.getTotalAmount(userId, EXPENSE, from, to);

        verify(transactionRepository, times(1))
                .sumAmountByUserAndTypeAndDateRange(userId, INCOME, from, to);
        verify(transactionRepository, times(1))
                .sumAmountByUserAndTypeAndDateRange(userId, EXPENSE, from, to);
    }

    @Test
    void getTotalAmount_differentRanges_doNotShareCache() {
        var from2 = from.minusDays(7);
        transactionService.getTotalAmount(userId, INCOME, from, to);
        transactionService.getTotalAmount(userId, INCOME, from2, to);

        verify(transactionRepository, times(1))
                .sumAmountByUserAndTypeAndDateRange(userId, INCOME, from, to);
        verify(transactionRepository, times(1))
                .sumAmountByUserAndTypeAndDateRange(userId, INCOME, from2, to);
    }

    @Test
    void createTransaction_evictsTotals() {
        transactionService.getTotalAmount(userId, INCOME, from, to); // заполнить кэш
        clearInvocations(transactionRepository);

        createTx(INCOME, BigDecimal.valueOf(500)); // evict

        transactionService.getTotalAmount(userId, INCOME, from, to);
        verify(transactionRepository, times(1))
                .sumAmountByUserAndTypeAndDateRange(userId, INCOME, from, to);
    }

    @Test
    void updateTransaction_evictsTotals() {
        var created = createTx(INCOME, BigDecimal.valueOf(500));
        transactionService.getTotalAmount(userId, INCOME, from, to);
        clearInvocations(transactionRepository);

        var dto = new TransactionDto();
        dto.setUserId(userId);
        dto.setAmount(BigDecimal.valueOf(1000));
        dto.setType(INCOME);
        dto.setCategoryId(category.getId());
        dto.setDate(LocalDateTime.now());
        dto.setDescription("Updated");
        transactionService.updateTransaction(created.getId(), dto); // evict

        transactionService.getTotalAmount(userId, INCOME, from, to);
        verify(transactionRepository, times(1))
                .sumAmountByUserAndTypeAndDateRange(userId, INCOME, from, to);
    }

    @Test
    void deleteTransaction_evictsTotals() {
        var created = createTx(INCOME, BigDecimal.valueOf(500));
        transactionService.getTotalAmount(userId, INCOME, from, to);
        clearInvocations(transactionRepository);

        transactionService.deleteTransaction(created.getId(), userId); // evict

        transactionService.getTotalAmount(userId, INCOME, from, to);
        verify(transactionRepository, times(1))
                .sumAmountByUserAndTypeAndDateRange(userId, INCOME, from, to);
    }

    private ru.vtvhw.spring.finance.entity.Transaction createTx(
            ru.vtvhw.spring.finance.enums.TransactionType type, BigDecimal amount) {
        var dto = new TransactionDto();
        dto.setUserId(userId);
        dto.setAmount(amount);
        dto.setType(type);
        dto.setCategoryId(category.getId());
        dto.setDate(LocalDateTime.now());
        dto.setDescription("Test");
        return transactionService.createTransaction(dto);
    }
}
