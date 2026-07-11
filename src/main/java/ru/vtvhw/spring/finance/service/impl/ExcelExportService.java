package ru.vtvhw.spring.finance.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import ru.vtvhw.spring.finance.dto.TransactionDto;
import ru.vtvhw.spring.finance.entity.Report;
import ru.vtvhw.spring.finance.entity.User;
import ru.vtvhw.spring.finance.enums.TransactionType;
import ru.vtvhw.spring.finance.service.ExportService;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;

import static java.math.BigDecimal.ZERO;
import static java.util.Objects.nonNull;
import static org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND;
import static org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER;
import static org.apache.poi.ss.usermodel.HorizontalAlignment.RIGHT;
import static org.apache.poi.ss.usermodel.IndexedColors.LIGHT_GREEN;
import static ru.vtvhw.spring.finance.enums.TransactionType.EXPENSE;
import static ru.vtvhw.spring.finance.enums.TransactionType.INCOME;

@Component("excelExportService")
@Slf4j
public class ExcelExportService implements ExportService {

    @Override
    public byte[] generateReport(User user, Report report, List<TransactionDto> transactions) throws Exception {
        var workbook = new XSSFWorkbook();
        var sheet = workbook.createSheet("Отчёт");

        var headerStyle = getHeaderStyle(workbook);
        var boldTextStyle = getBoldTextStyle(workbook);

        createHeaderRow(sheet, headerStyle);

        int rowNum = 1;
        for (var transaction : transactions) {
            createDataRow(sheet, rowNum++, transaction);
        }

        var totalIncome = calculateTotal(transactions, INCOME);
        var totalExpense = calculateTotal(transactions, EXPENSE);
        createSummaryRows(sheet, rowNum, totalIncome, totalExpense, boldTextStyle);

        autoSizeColumns(sheet);

        var out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();

        log.info("Excel report generated for user {}", user.getId());
        return out.toByteArray();
    }
    private void createHeaderRow(Sheet sheet, CellStyle style) {
        var headerRow = sheet.createRow(0);
        style.setAlignment(CENTER);

        var headers = new String[]{"Дата", "Категория", "Тип", "Сумма", "Описание"};
        for (int i = 0; i < headers.length; i++) {
            var cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(style);
        }
    }

    private void createDataRow(Sheet sheet, int rowNum, TransactionDto transaction) {
        var row = sheet.createRow(rowNum);
        var dateStr = nonNull(transaction.getDate())
                ? transaction.getDate().format(DATE_FORMATTER)
                : "";

        row.createCell(0).setCellValue(dateStr);
        row.createCell(1).setCellValue(nonNull(transaction.getCategoryName()) ? transaction.getCategoryName() : "");
        row.createCell(2).setCellValue(transaction.getType().getValue());
        row.createCell(3).setCellValue(transaction.getAmount().doubleValue());
        row.createCell(4).setCellValue(nonNull(transaction.getDescription()) ? transaction.getDescription() : "");
    }

    private BigDecimal calculateTotal(List<TransactionDto> transactions, TransactionType type) {
        return transactions.stream()
                .filter(t -> t.getType() == type)
                .map(TransactionDto::getAmount)
                .reduce(ZERO, BigDecimal::add);
    }

    private int createSummaryRows(Sheet sheet, int startRow, BigDecimal income, BigDecimal expense, CellStyle boldStyle) {
        int rowNum = startRow;

        rowNum = createSummaryRow(sheet, rowNum, "Итого доходов:", income, boldStyle);
        rowNum = createSummaryRow(sheet, rowNum, "Итого расходов:", expense, boldStyle);
        rowNum = createSummaryRow(sheet, rowNum, "Баланс:", income.add(expense), boldStyle);

        return rowNum;
    }

    private int createSummaryRow(Sheet sheet, int startRow, String text, BigDecimal value, CellStyle style) {
        int rowNum = startRow;

        style.setAlignment(RIGHT);

        var row = sheet.createRow(rowNum++);
        var cell = row.createCell(0);
        cell.setCellValue(text);
        cell.setCellStyle(style);
        row.createCell(3).setCellValue(value.doubleValue());

        return rowNum;
    }

    private void autoSizeColumns(Sheet sheet) {
        for (int i = 0; i < 5; i++) {
            sheet.autoSizeColumn(i);
            int currentWidth = sheet.getColumnWidth(i);
            sheet.setColumnWidth(i, Math.min(currentWidth + 512, 255 * 256));
        }
    }

    private CellStyle getHeaderStyle(Workbook workbook) {
        var style = getBoldTextStyle(workbook);
        style.setFillForegroundColor(LIGHT_GREEN.getIndex());
        style.setFillPattern(SOLID_FOREGROUND);
        return style;
    }

    private CellStyle getBoldTextStyle(Workbook workbook) {
        var style = workbook.createCellStyle();
        var font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }
}
