package ru.vtvhw.spring.finance.controller.rest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.vtvhw.spring.finance.entity.User;
import ru.vtvhw.spring.finance.service.TransactionService;
import ru.vtvhw.spring.finance.service.UserService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static java.math.BigDecimal.ZERO;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.vtvhw.spring.finance.enums.TransactionType.EXPENSE;
import static ru.vtvhw.spring.finance.enums.TransactionType.INCOME;

@WebMvcTest(AnalyticsController.class)
@WithMockUser(username = "test@example.com")
public class AnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private TransactionService transactionService;

    private final UUID userId = UUID.randomUUID();
    private final User user = User.builder().id(userId).name("Test User").email("test@example.com").build();

    @Test
    void getMetrics_ShouldReturnIncomeExpenseBalance() throws Exception {
        when(userService.getByEmail("test@example.com")).thenReturn(user);
        when(transactionService.getTotalAmount(eq(userId), eq(INCOME), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(BigDecimal.valueOf(1000));
        when(transactionService.getTotalAmount(eq(userId), eq(EXPENSE), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(BigDecimal.valueOf(-300));

        mockMvc.perform(get("/api/analytics/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.income").value(1000))
                .andExpect(jsonPath("$.expense").value(-300))
                .andExpect(jsonPath("$.balance").value(700));
    }

    @Test
    void getMetrics_WhenNoTransactions_ShouldReturnZero() throws Exception {
        when(userService.getByEmail("test@example.com")).thenReturn(user);
        when(transactionService.getTotalAmount(any(), any(), any(), any())).thenReturn(ZERO);

        mockMvc.perform(get("/api/analytics/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.income").value(0))
                .andExpect(jsonPath("$.expense").value(0))
                .andExpect(jsonPath("$.balance").value(0));
    }

    @Test
    void getMetrics_WhenIncomeOnly_ShouldReturnCorrectBalance() throws Exception {
        when(userService.getByEmail("test@example.com")).thenReturn(user);
        when(transactionService.getTotalAmount(eq(userId), eq(INCOME), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(BigDecimal.valueOf(500));
        when(transactionService.getTotalAmount(eq(userId), eq(EXPENSE), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(ZERO);

        mockMvc.perform(get("/api/analytics/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.income").value(500))
                .andExpect(jsonPath("$.expense").value(0))
                .andExpect(jsonPath("$.balance").value(500));
    }

    @Test
    void getMetrics_WhenExpenseOnly_ShouldReturnNegativeBalance() throws Exception {
        when(userService.getByEmail("test@example.com")).thenReturn(user);
        when(transactionService.getTotalAmount(eq(userId), eq(INCOME), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(ZERO);
        when(transactionService.getTotalAmount(eq(userId), eq(EXPENSE), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(BigDecimal.valueOf(-200));

        mockMvc.perform(get("/api/analytics/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.income").value(0))
                .andExpect(jsonPath("$.expense").value(-200))
                .andExpect(jsonPath("$.balance").value(-200));
    }
}
