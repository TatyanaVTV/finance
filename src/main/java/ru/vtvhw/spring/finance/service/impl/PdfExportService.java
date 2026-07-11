package ru.vtvhw.spring.finance.service.impl;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.vtvhw.spring.finance.dto.TransactionDto;
import ru.vtvhw.spring.finance.entity.Report;
import ru.vtvhw.spring.finance.entity.User;
import ru.vtvhw.spring.finance.service.ExportService;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static com.itextpdf.text.Element.ALIGN_CENTER;
import static ru.vtvhw.spring.finance.enums.TransactionType.EXPENSE;
import static ru.vtvhw.spring.finance.enums.TransactionType.INCOME;

@Component("pdfExportService")
@Slf4j
public class PdfExportService implements ExportService {

    @Override
    public byte[] generateReport(User user, Report report, List<TransactionDto> transactions) throws Exception {
        var out = new ByteArrayOutputStream();
        var document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, out);
        document.open();

        var title = new Paragraph("Финансовый отчёт");
        title.setAlignment(ALIGN_CENTER);
        document.add(title);

        document.add(new Paragraph("Пользователь: " + user.getName()));
        document.add(new Paragraph("Период: " + report.getStartDate() + " - " + report.getEndDate()));
        document.add(new Paragraph(" "));

        var table = new PdfPTable(5);
        table.addCell("Дата");
        table.addCell("Категория");
        table.addCell("Тип");
        table.addCell("Сумма");
        table.addCell("Описание");

        var formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        for (var transaction : transactions) {
            table.addCell(transaction.getDate().format(formatter));
            table.addCell(transaction.getCategoryName() != null ? transaction.getCategoryName() : "");
            table.addCell(transaction.getType().name());
            table.addCell(transaction.getAmount().toString());
            table.addCell(transaction.getDescription() != null ? transaction.getDescription() : "");
        }
        document.add(table);

        var totalIncome = transactions.stream()
                .filter(transaction -> transaction.getType() == INCOME)
                .map(TransactionDto::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        var totalExpense = transactions.stream()
                .filter(transaction -> transaction.getType() == EXPENSE)
                .map(TransactionDto::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        document.add(new Paragraph("Итого доходов: " + totalIncome));
        document.add(new Paragraph("Итого расходов: " + totalExpense));
        document.add(new Paragraph("Баланс: " + totalIncome.subtract(totalExpense)));

        document.close();
        log.info("PDF report generated for user {}", user.getId());
        return out.toByteArray();
    }
}
