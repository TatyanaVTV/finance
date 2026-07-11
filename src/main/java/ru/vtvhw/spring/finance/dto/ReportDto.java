package ru.vtvhw.spring.finance.dto;

import lombok.Data;
import ru.vtvhw.spring.finance.enums.ReportPeriod;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class ReportDto {
    private UUID id;
    private UUID userId;
    private ReportPeriod period;
    private LocalDateTime generatedAt;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
}
