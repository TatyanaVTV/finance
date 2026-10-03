package ru.vtvhw.spring.finance.dto.report;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import ru.vtvhw.spring.finance.enums.ExportFormat;
import ru.vtvhw.spring.finance.enums.ReportPeriod;
import ru.vtvhw.spring.finance.validation.ValidDateRange;

import java.time.LocalDateTime;

@Data
@ValidDateRange
public class ReportExportRequest {

    @NotNull(message = "Период обязателен")
    private ReportPeriod period;

    @NotNull(message = "Начало периода обязательно")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime from;

    @NotNull(message = "Окончание периода обязательно")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime to;

    @NotNull(message = "Формат экспорта обязателен")
    private ExportFormat format;
}
