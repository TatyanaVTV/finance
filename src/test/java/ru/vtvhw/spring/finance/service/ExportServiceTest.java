package ru.vtvhw.spring.finance.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.vtvhw.spring.finance.dto.TransactionDto;
import ru.vtvhw.spring.finance.entity.Report;
import ru.vtvhw.spring.finance.entity.User;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public abstract class ExportServiceTest {

    protected ExportService exportService;
    protected abstract void initService();

    @BeforeEach
    public void setUp() {
        initService();
    }

    @Test
    void generateReport_ReturnsNonEmptyBytes() throws Exception {
        var user = createTestUser();
        var report = createTestReport(user);
        var transactions = createTestTransactions(user);
        var bytes = exportService.generateReport(user, report, transactions);
        assertNotNull(bytes);
        assertTrue(bytes.length > 0, "Report bytes should not be empty");
    }

    @Test
    void generateReport_ValidReport_ContainsUserData() throws Exception {
        var user = createTestUser();
        var report = createTestReport(user);
        var transactions = createTestTransactions(user);
        var bytes = exportService.generateReport(user, report, transactions);
        validateContent(bytes, user, report, transactions);
    }

    protected abstract User createTestUser();
    protected abstract Report createTestReport(User user);
    protected abstract List<TransactionDto> createTestTransactions(User user);
    protected abstract void validateContent(byte[] bytes, User user, Report report, List<TransactionDto> transactions) throws Exception;
}
