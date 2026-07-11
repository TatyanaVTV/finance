package ru.vtvhw.spring.finance.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import ru.vtvhw.spring.finance.dto.TransactionDto;
import ru.vtvhw.spring.finance.entity.Report;
import ru.vtvhw.spring.finance.entity.User;
import ru.vtvhw.spring.finance.service.ExportService;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static ru.vtvhw.spring.finance.enums.TransactionType.EXPENSE;
import static ru.vtvhw.spring.finance.enums.TransactionType.INCOME;

@Component("excelExportService")
@Slf4j
public class ExcelExportService implements ExportService {

    @Override
    public byte[] generateReport(User user, Report report, List<TransactionDto> transactions) throws Exception {
        var workbook = new XSSFWorkbook();
        var sheet = workbook.createSheet("Отчёт");

        var headerRow = sheet.createRow(0);
        var headers = new String[]{"Дата", "Категория", "Тип", "Сумма", "Описание"};
        for (int i = 0; i < headers.length; i++) {
            var cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(getHeaderStyle(workbook));
        }

        var formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        int rowNum = 1;
        for (var transaction : transactions) {
            var row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(transaction.getDate().format(formatter));
            row.createCell(1).setCellValue(transaction.getCategoryName() != null ? transaction.getCategoryName() : "");
            row.createCell(2).setCellValue(transaction.getType().name());
            row.createCell(3).setCellValue(transaction.getAmount().doubleValue());
            row.createCell(4).setCellValue(transaction.getDescription() != null ? transaction.getDescription() : "");
        }

        var summaryRow = sheet.createRow(rowNum);
        summaryRow.createCell(0).setCellValue("Итого доходов:");
        var totalIncome = transactions.stream()
                .filter(transaction -> transaction.getType() == INCOME)
                .map(TransactionDto::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        summaryRow.createCell(3).setCellValue(totalIncome.doubleValue());

        var summaryRow2 = sheet.createRow(rowNum + 1);
        summaryRow2.createCell(0).setCellValue("Итого расходов:");
        var totalExpense = transactions.stream()
                .filter(transaction -> transaction.getType() == EXPENSE)
                .map(TransactionDto::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        summaryRow2.createCell(3).setCellValue(totalExpense.doubleValue());

        var summaryRow3 = sheet.createRow(rowNum + 2);
        summaryRow3.createCell(0).setCellValue("Баланс:");
        summaryRow3.createCell(3).setCellValue(totalIncome.subtract(totalExpense).doubleValue());

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        var out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();
        log.info("Excel report generated for user {}", user.getId());
        return out.toByteArray();
    }

    private CellStyle getHeaderStyle(Workbook workbook) {
        var style = workbook.createCellStyle();
        var font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }
}
