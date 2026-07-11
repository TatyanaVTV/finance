package ru.vtvhw.spring.finance.service;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.vtvhw.spring.finance.dto.TransactionDto;
import ru.vtvhw.spring.finance.entity.User;
import ru.vtvhw.spring.finance.enums.TransactionType;
import ru.vtvhw.spring.finance.exception.ResourceNotFoundException;
import ru.vtvhw.spring.finance.exception.ValidationException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static java.math.BigDecimal.ZERO;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.vtvhw.spring.finance.enums.ReportPeriod.MONTH;
import static ru.vtvhw.spring.finance.enums.TransactionType.EXPENSE;
import static ru.vtvhw.spring.finance.enums.TransactionType.INCOME;

public abstract class ReportServiceTest {

    protected ReportService reportService;

    protected abstract void initService();

    protected abstract User createTestUser(String name, String email, String password);

    protected abstract List<TransactionDto> createTestTransactions(User user);

    @BeforeEach
    public void setUp() {
        initService();
    }

    @Test
    void generateReport_Success() {
        var user = createTestUser("Report User", "report@example.com", "pass");
        var transactions = createTestTransactions(user);
        var start = LocalDateTime.now().minusMonths(1);
        var end = LocalDateTime.now();

        var report = reportService.generateReport(user, MONTH, start, end, transactions);

        var softly = new SoftAssertions();

        softly.assertThat(report).isNotNull();
        softly.assertThat(report.getId()).isNotNull();
        softly.assertThat(report.getUser().getId()).isEqualTo(user.getId());
        softly.assertThat(report.getPeriod()).isEqualTo(MONTH);
        softly.assertThat(report.getStartDate()).isEqualTo(start);
        softly.assertThat(report.getEndDate()).isEqualTo(end);
        softly.assertThat(report.getContent()).isNotNull();
        softly.assertThat(report.getContent()).contains("Доходы: ");
        softly.assertThat(report.getContent()).contains("Расходы: ");
        softly.assertThat(report.getContent()).contains("Баланс: ");

        softly.assertAll();
    }

    @Test
    void generateReport_StartAfterEnd_ThrowsException() {
        var user = createTestUser("Bad Report", "bad@example.com", "pass");
        var transactions = List.<TransactionDto>of();
        var start = LocalDateTime.now();
        var end = LocalDateTime.now().minusDays(1);

        assertThatThrownBy(() -> reportService.generateReport(user, MONTH, start, end, transactions))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Дата начала не может быть позже даты окончания");
    }

    @Test
    void generateReport_CalculatesTotalsCorrectly() {
        var user = createTestUser("Calc User", "calc@example.com", "pass");
        var transactions = createTestTransactions(user);
        var start = LocalDateTime.now().minusMonths(1);
        var end = LocalDateTime.now();

        var report = reportService.generateReport(user, MONTH, start, end, transactions);

        var totalIncome = transactions.stream()
                .filter(t -> t.getType() == INCOME)
                .map(TransactionDto::getAmount)
                .reduce(ZERO, BigDecimal::add);
        var totalExpense = transactions.stream()
                .filter(t -> t.getType() == EXPENSE)
                .map(TransactionDto::getAmount)
                .reduce(ZERO, BigDecimal::add);
        var balance = totalIncome.add(totalExpense);

        var softly = new SoftAssertions();

        softly.assertThat(report.getContent()).contains("Доходы: " + totalIncome);
        softly.assertThat(report.getContent()).contains("Расходы: " + totalExpense);
        softly.assertThat(report.getContent()).contains("Баланс: " + balance);

        softly.assertAll();
    }

    @Test
    void generateReport_WithEmptyTransactions_Works() {
        var user = createTestUser("Empty User", "empty@example.com", "pass");
        var transactions = List.<TransactionDto>of();
        var start = LocalDateTime.now().minusMonths(1);
        var end = LocalDateTime.now();

        var report = reportService.generateReport(user, MONTH, start, end, transactions);

        var softly = new SoftAssertions();

        softly.assertThat(report).isNotNull();
        softly.assertThat(report.getContent()).contains("Доходы: 0");
        softly.assertThat(report.getContent()).contains("Расходы: 0");
        softly.assertThat(report.getContent()).contains("Баланс: 0");

        softly.assertAll();
    }

    @Test
    void generateReport_OnlyIncome_Works() {
        var user = createTestUser("Income Only", "incomeOnly@example.com", "pass");
        var transactions = List.of(
                createTransactionDto(user, BigDecimal.valueOf(100), INCOME, "Salary"),
                createTransactionDto(user, BigDecimal.valueOf(50), INCOME, "Bonus")
        );
        var start = LocalDateTime.now().minusMonths(1);
        var end = LocalDateTime.now();

        var report = reportService.generateReport(user, MONTH, start, end, transactions);

        var softly = new SoftAssertions();

        softly.assertThat(report.getContent()).contains("Доходы: 150");
        softly.assertThat(report.getContent()).contains("Расходы: 0");
        softly.assertThat(report.getContent()).contains("Баланс: 150");

        softly.assertAll();
    }

    @Test
    void generateReport_OnlyExpense_Works() {
        var user = createTestUser("Expense Only", "expenseOnly@example.com", "pass");
        var transactions = List.of(
                createTransactionDto(user, BigDecimal.valueOf(-50), EXPENSE, "Food"),
                createTransactionDto(user, BigDecimal.valueOf(-30), EXPENSE, "Transport")
        );
        var start = LocalDateTime.now().minusMonths(1);
        var end = LocalDateTime.now();

        var report = reportService.generateReport(user, MONTH, start, end, transactions);

        var softly = new SoftAssertions();

        softly.assertThat(report.getContent()).contains("Доходы: 0");
        softly.assertThat(report.getContent()).contains("Расходы: -80");
        softly.assertThat(report.getContent()).contains("Баланс: -80");

        softly.assertAll();
    }

    @Test
    void getById_Success() {
        var user = createTestUser("Get User", "get@example.com", "pass");
        var transactions = createTestTransactions(user);
        var start = LocalDateTime.now().minusMonths(1);
        var end = LocalDateTime.now();

        var saved = reportService.generateReport(user, MONTH, start, end, transactions);
        var found = reportService.getById(saved.getId());

        var softly = new SoftAssertions();

        softly.assertThat(found.getId()).isEqualTo(saved.getId());
        softly.assertThat(found.getContent()).isEqualTo(saved.getContent());

        softly.assertAll();
    }

    @Test
    void getById_NotFound_ThrowsException() {
        assertThatThrownBy(() -> reportService.getById(UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Отчёт не найден");
    }

    protected TransactionDto createTransactionDto(User user, BigDecimal amount, TransactionType type, String description) {
        var dto = new TransactionDto();
        dto.setUserId(user.getId());
        dto.setAmount(amount);
        dto.setType(type);
        dto.setDate(LocalDateTime.now());
        dto.setDescription(description);

        return dto;
    }
}
