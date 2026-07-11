package ru.vtvhw.spring.finance.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import ru.vtvhw.spring.finance.dto.TransactionDto;
import ru.vtvhw.spring.finance.entity.Report;
import ru.vtvhw.spring.finance.entity.User;
import ru.vtvhw.spring.finance.service.*;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.vtvhw.spring.finance.enums.ReportPeriod.MONTH;
import static ru.vtvhw.spring.finance.enums.TransactionType.INCOME;

@SpringBootTest
@ActiveProfiles("test")
public class ExcelExportServiceTest extends ExportServiceTest {

    @Autowired
    @Qualifier("excelExportService")
    private ExportService excelExportService;

    @Autowired
    private UserService userService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private TransactionService transactionService;

    @Override
    protected void initService() {
        this.exportService = excelExportService;
    }

    @Override
    protected User createTestUser() {
        var email = "excel-test-" + System.currentTimeMillis() + "@example.com";
        return userService.createUser("Excel User", email, "password");
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
    protected void validateContent(byte[] bytes, User user, Report report, List<TransactionDto> transactions) throws Exception {
        var header = new String(bytes, 0, 2);
        assertTrue(header.startsWith("PK"), "Excel should be a ZIP archive (PK header)");

        var foundWorkbook = false;
        try (var zis = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            var entry = zis.getNextEntry();
            while (entry != null) {
                if (entry.getName().equals("xl/workbook.xml")) {
                    foundWorkbook = true;
                    break;
                }
                entry = zis.getNextEntry();
            }
        }
        assertTrue(foundWorkbook, "Excel archive should contain xl/workbook.xml");
        // на данный момент - упрощенная базовая проверка
    }
}
