package ru.vtvhw.spring.finance.service;

import ru.vtvhw.spring.finance.dto.TransactionDto;
import ru.vtvhw.spring.finance.entity.Report;
import ru.vtvhw.spring.finance.entity.User;

import java.util.List;

public interface ExportService {
    byte[] generateReport(User user, Report report, List<TransactionDto> transactions) throws Exception;
}
