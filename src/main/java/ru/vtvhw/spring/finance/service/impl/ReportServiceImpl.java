package ru.vtvhw.spring.finance.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.vtvhw.spring.finance.dto.TransactionDto;
import ru.vtvhw.spring.finance.entity.Report;
import ru.vtvhw.spring.finance.entity.User;
import ru.vtvhw.spring.finance.enums.ReportPeriod;
import ru.vtvhw.spring.finance.repository.ReportRepository;
import ru.vtvhw.spring.finance.service.ReportService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static java.math.BigDecimal.ZERO;
import static ru.vtvhw.spring.finance.enums.TransactionType.EXPENSE;
import static ru.vtvhw.spring.finance.enums.TransactionType.INCOME;
import static ru.vtvhw.spring.finance.exception.ResourceNotFoundException.reportNotFound;
import static ru.vtvhw.spring.finance.exception.ValidationException.invalidDateRange;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportServiceImpl implements ReportService {
    private final ReportRepository reportRepository;

    @Override
    public Report generateReport(User user, ReportPeriod period,
                                 LocalDateTime start, LocalDateTime end,
                                 List<TransactionDto> transactions) {
        if (start.isAfter(end)) {
            throw invalidDateRange();
        }

        var totalIncome = getTotalIncome(transactions);
        var totalExpense = getTotalExpense(transactions);
        var balance = totalIncome.subtract(totalExpense);

        var content = String.format("Отчет за период %s - %s. Доходы: %s, Расходы: %s, Баланс: %s",
                start, end, totalIncome, totalExpense, balance);

        var report = Report.builder()
                .user(user)
                .period(period)
                .startDate(start)
                .endDate(end)
                .content(content)
                .build();
        var saved = reportRepository.save(report);
        log.info("Report generated for user {} with id {}", user.getId(), saved.getId());
        return saved;
    }

    @Override
    public Report getById(UUID id) {
        return reportRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Report not found: {}", id);
                    return reportNotFound(id);
                });
    }

    private BigDecimal getTotalIncome(List<TransactionDto> transactions) {
        return transactions.stream()
                .filter(transaction -> transaction.getType() == INCOME)
                .map(TransactionDto::getAmount)
                .reduce(ZERO, BigDecimal::add);
    }

    private BigDecimal getTotalExpense(List<TransactionDto> transactions) {
        return transactions.stream()
                .filter(transaction -> transaction.getType() == EXPENSE)
                .map(TransactionDto::getAmount)
                .reduce(ZERO, BigDecimal::add);
    }
}
