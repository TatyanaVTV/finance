package ru.vtvhw.spring.finance.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ru.vtvhw.spring.finance.dto.TransactionDto;
import ru.vtvhw.spring.finance.enums.ExportFormat;
import ru.vtvhw.spring.finance.enums.ReportPeriod;
import ru.vtvhw.spring.finance.mapper.TransactionMapper;
import ru.vtvhw.spring.finance.service.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.Objects.isNull;
import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;
import static org.springframework.http.MediaType.APPLICATION_PDF_VALUE;
import static ru.vtvhw.spring.finance.enums.ExportFormat.PDF;
import static ru.vtvhw.spring.finance.enums.TransactionType.EXPENSE;
import static ru.vtvhw.spring.finance.enums.TransactionType.INCOME;
import static ru.vtvhw.spring.finance.exception.ExportException.notSupported;
import static ru.vtvhw.spring.finance.exception.ValidationException.invalidDateRange;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class FinanceRestController {

    private final TransactionService transactionService;
    private final ReportService reportService;
    private final UserService userService;
    private final TransactionMapper transactionMapper;
    private final Map<String, ExportService> exportServices;

    /**
     * Получение транзакций за период (по умолчанию – за последний месяц).
     * Используется для AJAX-обновления списка на страницах.
     */
    @GetMapping("/transactions")
    public List<TransactionDto> getTransactions(Authentication auth,
                                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
                                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        var user = userService.getByEmail(auth.getName());
        if (isNull(from)) from = LocalDateTime.now().minusMonths(1);
        if (isNull(to)) to = LocalDateTime.now();
        return transactionService.getTransactionsForUser(user.getId(), from, to);
    }

    /**
     * Добавление новой транзакции через AJAX (JSON).
     */
    @PostMapping("/transactions")
    public TransactionDto addTransaction(Authentication auth, @RequestBody TransactionDto dto) {
        var user = userService.getByEmail(auth.getName());
        dto.setUserId(user.getId());
        var created = transactionService.createTransaction(dto);
        return transactionMapper.toDto(created);
    }

    /**
     * Экспорт отчёта в PDF или Excel.
     * Выбор реализации экспорта происходит по имени бина: "pdfExportService" или "excelExportService".
     */
    @GetMapping("/reports/export")
    public ResponseEntity<byte[]> exportReport(Authentication auth,
                                               @RequestParam ReportPeriod period,
                                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
                                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
                                               @RequestParam ExportFormat format) throws Exception {

        if (from.isAfter(to)) {
            throw invalidDateRange();
        }

        var user = userService.getByEmail(auth.getName());
        var transactions = transactionService.getTransactionsForUser(user.getId(), from, to);
        var report = reportService.generateReport(user, period, from, to, transactions);

        var beanName = format.name().toLowerCase() + "ExportService";
        var exportService = exportServices.get(beanName);
        if (isNull(exportService)) {
            throw notSupported(format);
        }

        var content = exportService.generateReport(user, report, transactions);

        String contentType;
        String fileName;
        switch (format) {
            case PDF -> {
                contentType = APPLICATION_PDF_VALUE;
                fileName = "report.pdf";
            }
            case EXCEL -> {
                contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                fileName = "report.xlsx";
            }
            default -> throw notSupported(format);
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(CONTENT_DISPOSITION, "attachment; filename=" + fileName)
                .body(content);
    }

    /**
     * Получение ключевых метрик (доходы, расходы, баланс) для динамического обновления дашборда.
     * Используется для AJAX-подгрузки на главной странице.
     */
    @GetMapping("/analytics/metrics")
    public Map<String, Object> getMetrics(Authentication auth) {
        var user = userService.getByEmail(auth.getName());
        var now = LocalDateTime.now();
        var startOfMonth = now.withDayOfMonth(1);
        var endOfMonth = now.withDayOfMonth(now.toLocalDate().lengthOfMonth());

        var income = transactionService.getTotalAmount(user.getId(), INCOME, startOfMonth, endOfMonth);
        var expense = transactionService.getTotalAmount(user.getId(), EXPENSE, startOfMonth, endOfMonth);
        return Map.of(
                "income", income,
                "expense", expense,
                "balance", income.subtract(expense)
        );
    }

    /**
     * Обновление транзакции
     */
    @PutMapping("/transactions/{id}")
    public TransactionDto updateTransaction(Authentication auth,
                                            @PathVariable UUID id,
                                            @RequestBody TransactionDto dto) {
        var user = userService.getByEmail(auth.getName());
        dto.setUserId(user.getId());
        var updated = transactionService.updateTransaction(id, dto);
        return transactionMapper.toDto(updated);
    }

    /**
     * Удаление транзакции
     */
    @DeleteMapping("/transactions/{id}")
    public ResponseEntity<Void> deleteTransaction(Authentication auth,
                                                  @PathVariable UUID id) {
        var user = userService.getByEmail(auth.getName());
        transactionService.deleteTransaction(id, user.getId());
        return ResponseEntity.noContent().build();
    }

    /**
     * Получение одной транзакции по id
     */
    @GetMapping("/transactions/{id}")
    public TransactionDto getTransactionById(Authentication auth, @PathVariable UUID id) {
        var user = userService.getByEmail(auth.getName());
        return transactionService.getTransactionById(id, user.getId());
    }
}
