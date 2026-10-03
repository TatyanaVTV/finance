package ru.vtvhw.spring.finance.service.impl;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;
import ru.vtvhw.spring.finance.dto.transaction.TransactionDto;
import ru.vtvhw.spring.finance.entity.User;
import ru.vtvhw.spring.finance.enums.TransactionType;
import ru.vtvhw.spring.finance.repository.CategoryRepository;
import ru.vtvhw.spring.finance.service.CategoryService;
import ru.vtvhw.spring.finance.service.CategoryServiceTest;
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
public class CategoryServiceImplTest extends CategoryServiceTest {
    private User testUser;

    @Autowired
    private CategoryService categoryServiceImpl;

    @MockitoSpyBean
    private CategoryRepository categoryRepository;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private UserService userService;

    @Override
    protected void initService() {
        this.categoryService = categoryServiceImpl;
    }

    @Override
    protected User createTestUser() {
        if (testUser == null) {
            var email = format("test-category-%s@example.com", System.currentTimeMillis());
            testUser = userService.createUser("Test User", email, "password");
        }
        return testUser;
    }

    @Override
    protected void createTransactionForCategory(UUID userId, UUID categoryId, TransactionType type) {
        var dto = new TransactionDto();
        dto.setUserId(userId);
        dto.setAmount(BigDecimal.valueOf(100));
        dto.setType(type);
        dto.setCategoryId(categoryId);
        dto.setDate(LocalDateTime.now());
        dto.setDescription("Test transaction");
        transactionService.createTransaction(dto);
    }

    @Test
    void getCategoriesByUser_SecondCall_UsesCache() {
        // подготовка: категории уже в БД
        categoryService.createCategory("Cached1", INCOME, userId);
        categoryService.createCategory("Cached2", EXPENSE, userId);

        // Очищаем счётчик вызовов — createCategory выше тоже дёргал репозиторий
        clearInvocations(categoryRepository);

        // Два вызова подряд
        categoryService.getCategoriesByUser(userId);
        categoryService.getCategoriesByUser(userId);

        // Репозиторий должен дёрнуться только один раз — второй раз из кэша
        verify(categoryRepository, times(1)).findByUserId(userId);
    }
}
