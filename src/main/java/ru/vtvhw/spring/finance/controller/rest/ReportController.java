package ru.vtvhw.spring.finance.controller.rest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.vtvhw.spring.finance.enums.ExportFormat;
import ru.vtvhw.spring.finance.enums.ReportPeriod;
import ru.vtvhw.spring.finance.service.ExportService;
import ru.vtvhw.spring.finance.service.ReportService;
import ru.vtvhw.spring.finance.service.TransactionService;
import ru.vtvhw.spring.finance.service.UserService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static java.util.Objects.isNull;
import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;
import static org.springframework.http.MediaType.APPLICATION_PDF_VALUE;
import static ru.vtvhw.spring.finance.exception.ExportException.notSupported;
import static ru.vtvhw.spring.finance.exception.ValidationException.invalidDateRange;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Slf4j
public class ReportController {
    private final UserService userService;
    private final TransactionService transactionService;
    private final ReportService reportService;
    private final Map<String, ExportService> exportServices;

    /**
     * Экспорт отчёта в PDF или Excel.
     * Выбор реализации экспорта происходит по имени бина: "pdfExportService" или "excelExportService".
     */
    @GetMapping("/export")
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

        var fileName = buildReportName(from, to, format);
        var contentType = switch (format) {
            case PDF -> APPLICATION_PDF_VALUE;
            case EXCEL -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        };

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(CONTENT_DISPOSITION, "attachment; filename=" + fileName)
                .body(content);
    }

    private String buildReportName(LocalDateTime from, LocalDateTime to, ExportFormat format) {
        var dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        var fromStr = from.format(dateFormatter);
        var toStr = to.format(dateFormatter);

        var extension = switch (format) {
            case PDF -> "pdf";
            case EXCEL -> "xlsx";
        };

        return String.format("financialReport_%s_%s.%s", fromStr, toStr, extension);
    }
}
