package ru.vtvhw.spring.finance.service;

import ru.vtvhw.spring.finance.dto.TransactionDto;
import ru.vtvhw.spring.finance.entity.Report;
import ru.vtvhw.spring.finance.entity.User;
import ru.vtvhw.spring.finance.enums.ReportPeriod;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ReportService {
    Report generateReport(User user, ReportPeriod period, LocalDateTime start, LocalDateTime end, List<TransactionDto> transactions);
    Report getById(UUID id);
}
