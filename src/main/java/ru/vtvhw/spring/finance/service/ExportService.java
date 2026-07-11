package ru.vtvhw.spring.finance.service;

import ru.vtvhw.spring.finance.dto.TransactionDto;
import ru.vtvhw.spring.finance.entity.Report;
import ru.vtvhw.spring.finance.entity.User;

import java.time.format.DateTimeFormatter;
import java.util.List;

public interface ExportService {
    DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    byte[] generateReport(User user, Report report, List<TransactionDto> transactions) throws Exception;
}
