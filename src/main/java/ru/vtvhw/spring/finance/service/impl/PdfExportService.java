package ru.vtvhw.spring.finance.service.impl;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.vtvhw.spring.finance.dto.TransactionDto;
import ru.vtvhw.spring.finance.entity.Report;
import ru.vtvhw.spring.finance.entity.User;
import ru.vtvhw.spring.finance.enums.TransactionType;
import ru.vtvhw.spring.finance.service.ExportService;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;

import static com.itextpdf.text.BaseColor.LIGHT_GRAY;
import static com.itextpdf.text.Element.ALIGN_CENTER;
import static com.itextpdf.text.Font.BOLD;
import static com.itextpdf.text.PageSize.A4;
import static com.itextpdf.text.pdf.BaseFont.EMBEDDED;
import static com.itextpdf.text.pdf.BaseFont.IDENTITY_H;
import static java.lang.String.format;
import static java.math.BigDecimal.ZERO;
import static java.util.Objects.nonNull;
import static ru.vtvhw.spring.finance.enums.TransactionType.EXPENSE;
import static ru.vtvhw.spring.finance.enums.TransactionType.INCOME;

@Component("pdfExportService")
@Slf4j
public class PdfExportService implements ExportService {
    private static final String FONT_FILENAME = "/fonts/Merriweather-VariableFont_opsz,wdth,wght.ttf";
    private static final Font CYRILLIC_FONT;
    private static final Font CYRILLIC_FONT_BOLD;

    static {
        try {
            var baseFont = BaseFont.createFont(FONT_FILENAME, IDENTITY_H, EMBEDDED);
            CYRILLIC_FONT = new Font(baseFont, 11);
            CYRILLIC_FONT_BOLD = new Font(baseFont, 13, BOLD);
        } catch (Exception e) {
            log.error("Failed to load font for PDF", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte[] generateReport(User user, Report report, List<TransactionDto> transactions) throws Exception {
        var out = new ByteArrayOutputStream();
        var document = new Document(A4);
        PdfWriter.getInstance(document, out);
        document.open();

        addTitle(document, "Финансовый отчёт");
        addUserAndPeriod(document, user, report);

        addTransactionTable(document, transactions);

        var totalIncome = calculateTotal(transactions, INCOME);
        var totalExpense = calculateTotal(transactions, EXPENSE);
        addSummaries(document, totalIncome, totalExpense);

        document.close();
        log.info("PDF report generated for user {}", user.getId());
        return out.toByteArray();
    }

    private void addTitle(Document document, String title) throws DocumentException {
        var paragraph = new Paragraph(title, CYRILLIC_FONT_BOLD);
        paragraph.setAlignment(ALIGN_CENTER);
        paragraph.setSpacingAfter(20);
        document.add(paragraph);
    }

    private void addUserAndPeriod(Document document, User user, Report report) throws DocumentException {
        var userPhrase = new Phrase();
        userPhrase.add(new Chunk("Пользователь: ", CYRILLIC_FONT_BOLD));
        userPhrase.add(new Chunk(user.getName(), CYRILLIC_FONT));
        document.add(new Paragraph(userPhrase));

        String periodStr;
        var periodPhrase = new Phrase();
        periodPhrase.add(new Chunk("Период: ", CYRILLIC_FONT_BOLD));
        if (nonNull(report.getStartDate()) && nonNull(report.getEndDate())) {
            var startDate = report.getStartDate().format(DATE_FORMATTER);
            var endDate = report.getEndDate().format(DATE_FORMATTER);
            periodStr = format("%s - %s", startDate, endDate);
        } else {
            periodStr = "не указан";
        }
        periodPhrase.add(new Chunk(periodStr, CYRILLIC_FONT));
        document.add(new Paragraph(periodPhrase));
        document.add(new Paragraph(" ")); // отступ
    }

    private void addTransactionTable(Document document, List<TransactionDto> transactions) throws DocumentException {
        var table = new PdfPTable(5);
        table.setWidthPercentage(100);

        addHeaderCells(table, "Дата", "Категория", "Тип", "Сумма", "Описание");

        for (var t : transactions) {
            addTransactionRow(table, t);
        }

        document.add(table);
    }

    private void addHeaderCells(PdfPTable table, String... headers) {
        for (String header : headers) {
            var cell = new PdfPCell(new Phrase(header, CYRILLIC_FONT_BOLD));
            cell.setBackgroundColor(LIGHT_GRAY);
            table.addCell(cell);
        }
    }

    private void addTransactionRow(PdfPTable table, TransactionDto t) {
        var dateStr = nonNull(t.getDate()) ? t.getDate().format(DATE_FORMATTER) : "";
        var typeStr = nonNull(t.getType()) ? t.getType().getValue() : "";

        addCell(table, dateStr);
        addCell(table, nonNull(t.getCategoryName()) ? t.getCategoryName() : "");
        addCell(table, typeStr);
        addCell(table, nonNull(t.getAmount()) ? t.getAmount().toString() : "0");
        addCell(table, nonNull(t.getDescription()) ? t.getDescription() : "");
    }

    private void addCell(PdfPTable table, String text) {
        table.addCell(new Phrase(text, CYRILLIC_FONT));
    }

    private BigDecimal calculateTotal(List<TransactionDto> transactions, TransactionType type) {
        return transactions.stream()
                .filter(t -> t.getType() == type)
                .map(TransactionDto::getAmount)
                .reduce(ZERO, BigDecimal::add);
    }

    private void addSummaries(Document document, BigDecimal income, BigDecimal expense) throws DocumentException {
        var balance = income.add(expense);
        document.add(new Paragraph(" ")); // отступ
        addSummary(document, "Итого доходов: ", income);
        addSummary(document, "Итого расходов: ", expense);
        addSummary(document, "Баланс: ", balance);
    }

    private void addSummary(Document document, String label, BigDecimal value) throws DocumentException {
        var phrase = new Phrase();
        phrase.add(new Chunk(label, CYRILLIC_FONT_BOLD));
        phrase.add(new Chunk(value.toString(), CYRILLIC_FONT));
        document.add(new Paragraph(phrase));
    }
}