package ru.vtvhw.spring.finance.controller.rest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.vtvhw.spring.finance.dto.TransactionDto;
import ru.vtvhw.spring.finance.entity.Report;
import ru.vtvhw.spring.finance.entity.User;
import ru.vtvhw.spring.finance.enums.ReportPeriod;
import ru.vtvhw.spring.finance.service.ExportService;
import ru.vtvhw.spring.finance.service.ReportService;
import ru.vtvhw.spring.finance.service.TransactionService;
import ru.vtvhw.spring.finance.service.UserService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_PDF;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static ru.vtvhw.spring.finance.enums.ExportFormat.EXCEL;
import static ru.vtvhw.spring.finance.enums.ExportFormat.PDF;

@WebMvcTest(ReportController.class)
@WithMockUser(username = "test@example.com")
public class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private TransactionService transactionService;

    @MockitoBean
    private ReportService reportService;

    @MockitoBean
    private Map<String, ExportService> exportServices;

    private final UUID userId = UUID.randomUUID();
    private final User user = User.builder().id(userId).name("Test User").email("test@example.com").build();
    private final LocalDateTime from = LocalDateTime.of(2026, 1, 1, 0, 0);
    private final LocalDateTime to = LocalDateTime.of(2026, 1, 31, 23, 59);
    private final ReportPeriod period = ReportPeriod.MONTH;

    private final byte[] pdfBytes = "PDF content".getBytes();
    private final byte[] excelBytes = "Excel content".getBytes();

    @BeforeEach
    void setUp() {
        when(userService.getByEmail("test@example.com")).thenReturn(user);
        when(transactionService.getTransactionsForUser(eq(userId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(new TransactionDto()));
        when(reportService.generateReport(eq(user), eq(period), any(LocalDateTime.class), any(LocalDateTime.class), any()))
                .thenReturn(Report.builder().id(UUID.randomUUID()).user(user).period(period).startDate(from).endDate(to).build());
    }

    @Test
    void exportReport_Pdf_Success() throws Exception {
        var pdfService = mock(ExportService.class);
        when(exportServices.get("pdfExportService")).thenReturn(pdfService);
        when(pdfService.generateReport(any(), any(), any())).thenReturn(pdfBytes);

        mockMvc.perform(get("/api/reports/export")
                        .param("period", period.name())
                        .param("from", from.format(ISO_LOCAL_DATE_TIME))
                        .param("to", to.format(ISO_LOCAL_DATE_TIME))
                        .param("format", PDF.name())
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=financialReport_01-01-2026_31-01-2026.pdf"))
                .andExpect(content().contentType(APPLICATION_PDF))
                .andExpect(content().bytes(pdfBytes));
    }

    @Test
    void exportReport_Excel_Success() throws Exception {
        var excelService = mock(ExportService.class);
        when(exportServices.get("excelExportService")).thenReturn(excelService);
        when(excelService.generateReport(any(), any(), any())).thenReturn(excelBytes);

        mockMvc.perform(get("/api/reports/export")
                        .param("period", period.name())
                        .param("from", from.format(ISO_LOCAL_DATE_TIME))
                        .param("to", to.format(ISO_LOCAL_DATE_TIME))
                        .param("format", EXCEL.name())
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=financialReport_01-01-2026_31-01-2026.xlsx"))
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(content().bytes(excelBytes));
    }

    @Test
    void exportReport_InvalidDateRange_ThrowsValidationException() throws Exception {
        var fromAfterTo = LocalDateTime.of(2026, 2, 1, 0, 0);
        var toBeforeFrom = LocalDateTime.of(2026, 1, 31, 23, 59);

        mockMvc.perform(get("/api/reports/export")
                        .param("period", period.name())
                        .param("from", fromAfterTo.format(ISO_LOCAL_DATE_TIME))
                        .param("to", toBeforeFrom.format(ISO_LOCAL_DATE_TIME))
                        .param("format", PDF.name())
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Дата начала не может быть позже даты окончания"));
    }

    @Test
    void exportReport_UnsupportedFormat_ThrowsExportException() throws Exception {
        when(exportServices.get("pdfExportService")).thenReturn(null);

        mockMvc.perform(get("/api/reports/export")
                        .param("period", period.name())
                        .param("from", from.format(ISO_LOCAL_DATE_TIME))
                        .param("to", to.format(ISO_LOCAL_DATE_TIME))
                        .param("format", PDF.name())
                        .with(csrf()))
                .andExpect(status().isNotImplemented())
                .andExpect(jsonPath("$.error").value("Формат 'PDF' не поддерживается"));
    }

    @Test
    void exportReport_ExportServiceThrowsException_ShouldPropagate() throws Exception {
        var pdfService = mock(ExportService.class);
        when(exportServices.get("pdfExportService")).thenReturn(pdfService);
        when(pdfService.generateReport(any(), any(), any()))
                .thenThrow(new RuntimeException("Export error"));

        mockMvc.perform(get("/api/reports/export")
                        .param("period", period.name())
                        .param("from", from.format(ISO_LOCAL_DATE_TIME))
                        .param("to", to.format(ISO_LOCAL_DATE_TIME))
                        .param("format", PDF.name())
                        .with(csrf()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Внутренняя ошибка сервера"));
    }
}
