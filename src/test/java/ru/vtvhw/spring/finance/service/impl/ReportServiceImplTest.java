package ru.vtvhw.spring.finance.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.vtvhw.spring.finance.dto.TransactionDto;
import ru.vtvhw.spring.finance.entity.User;
import ru.vtvhw.spring.finance.service.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static ru.vtvhw.spring.finance.enums.TransactionType.EXPENSE;
import static ru.vtvhw.spring.finance.enums.TransactionType.INCOME;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class ReportServiceImplTest extends ReportServiceTest {

    @Autowired
    private ReportService reportServiceImpl;

    @Autowired
    private UserService userService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private TransactionService transactionService;

    @Override
    protected void initService() {
        this.reportService = reportServiceImpl;
    }

    @Override
    protected User createTestUser(String name, String email, String password) {
        var uniqueEmail = email + "-" + System.currentTimeMillis();
        return userService.createUser(name, uniqueEmail, password);
    }

    @Override
    protected List<TransactionDto> createTestTransactions(User user) {
        var categoryIncome = categoryService.createCategory("Salary", INCOME, user.getId());
        var categoryExpense = categoryService.createCategory("Food", EXPENSE, user.getId());

        var dto1 = new TransactionDto();
        dto1.setUserId(user.getId());
        dto1.setAmount(BigDecimal.valueOf(1000));
        dto1.setType(INCOME);
        dto1.setCategoryId(categoryIncome.getId());
        dto1.setDate(LocalDateTime.now().minusDays(5));
        dto1.setDescription("Salary");
        transactionService.createTransaction(dto1);

        var dto2 = new TransactionDto();
        dto2.setUserId(user.getId());
        dto2.setAmount(BigDecimal.valueOf(-200));
        dto2.setType(EXPENSE);
        dto2.setCategoryId(categoryExpense.getId());
        dto2.setDate(LocalDateTime.now().minusDays(3));
        dto2.setDescription("Groceries");
        transactionService.createTransaction(dto2);

        return List.of(dto1, dto2);
    }
}
