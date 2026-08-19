package ru.vtvhw.spring.finance.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import ru.vtvhw.spring.finance.dto.TransactionDto;
import ru.vtvhw.spring.finance.entity.Report;
import ru.vtvhw.spring.finance.entity.User;
import ru.vtvhw.spring.finance.service.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.vtvhw.spring.finance.enums.ReportPeriod.MONTH;
import static ru.vtvhw.spring.finance.enums.TransactionType.INCOME;

@SpringBootTest
@ActiveProfiles("test")
public class PdfExportServiceTest extends ExportServiceTest {

    @Autowired
    @Qualifier("pdfExportService")
    private ExportService pdfExportService;

    @Autowired
    private UserService userService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private TransactionService transactionService;

    @Override
    protected void initService() {
        this.exportService = pdfExportService;
    }

    @Override
    protected User createTestUser() {
        var email = "pdf-test-" + System.currentTimeMillis() + "@example.com";
        return userService.createUser("PDF User", email, "password");
    }

    @Override
    protected Report createTestReport(User user) {
        return Report.builder()
                .id(UUID.randomUUID())
                .user(user)
                .period(MONTH)
                .startDate(LocalDateTime.now().minusMonths(1))
                .endDate(LocalDateTime.now())
                .content("Test report content")
                .build();
    }

    @Override
    protected List<TransactionDto> createTestTransactions(User user) {
        var category = categoryService.createCategory("Salary", INCOME, user.getId());
        var dto = new TransactionDto();
        dto.setUserId(user.getId());
        dto.setAmount(BigDecimal.valueOf(1000));
        dto.setType(INCOME);
        dto.setCategoryId(category.getId());
        dto.setDate(LocalDateTime.now().minusDays(5));
        dto.setDescription("Test income");
        transactionService.createTransaction(dto);
        return List.of(dto);
    }

    @Override
    protected void validateContent(byte[] bytes, User user, Report report, List<TransactionDto> transactions) {
        var header = new String(bytes, 0, Math.min(bytes.length, 5));
        assertTrue(header.startsWith("%PDF"), "PDF should start with %PDF header");
        // на данный момент - упрощенная базовая проверка
    }
}
