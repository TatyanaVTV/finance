package ru.vtvhw.spring.finance.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.vtvhw.spring.finance.dto.TransactionDto;
import ru.vtvhw.spring.finance.entity.Category;
import ru.vtvhw.spring.finance.entity.User;
import ru.vtvhw.spring.finance.enums.TransactionType;
import ru.vtvhw.spring.finance.service.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class TransactionServiceImplTest extends TransactionServiceTest {

    @Autowired
    private TransactionService transactionServiceImpl;

    @Autowired
    private UserService userService;

    @Autowired
    private CategoryService categoryService;

    @Override
    protected void initService() {
        this.transactionService = transactionServiceImpl;
    }

    @Override
    protected User createTestUser(String name, String email, String password) {
        return userService.createUser(name, email, password);
    }

    @Override
    protected Category createTestCategory(String name, TransactionType type, User user) {
        return categoryService.createCategory(name, type, user.getId());
    }

    @Override
    protected TransactionDto createTestTransactionDto(User user,
                                                      Category category,
                                                      BigDecimal amount,
                                                      TransactionType type,
                                                      String description) {
        var dto = new TransactionDto();
        dto.setUserId(user.getId());
        dto.setAmount(amount);
        dto.setType(type);
        if (category != null) {
            dto.setCategoryId(category.getId());
        }
        dto.setDate(LocalDateTime.now());
        dto.setDescription(description);
        return dto;
    }
}
