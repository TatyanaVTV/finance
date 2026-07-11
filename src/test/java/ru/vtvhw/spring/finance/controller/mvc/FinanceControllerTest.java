package ru.vtvhw.spring.finance.controller.mvc;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.vtvhw.spring.finance.dto.CategoryDto;
import ru.vtvhw.spring.finance.dto.TransactionDto;
import ru.vtvhw.spring.finance.entity.Transaction;
import ru.vtvhw.spring.finance.entity.User;
import ru.vtvhw.spring.finance.enums.ReportPeriod;
import ru.vtvhw.spring.finance.exception.ValidationException;
import ru.vtvhw.spring.finance.service.CategoryService;
import ru.vtvhw.spring.finance.service.TransactionService;
import ru.vtvhw.spring.finance.service.UserService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static ru.vtvhw.spring.finance.enums.TransactionType.EXPENSE;
import static ru.vtvhw.spring.finance.enums.TransactionType.INCOME;

@WebMvcTest(FinanceController.class)
@WithMockUser(username = "test@example.com")
public class FinanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionService transactionService;

    @MockitoBean
    private CategoryService categoryService;

    @MockitoBean
    private UserService userService;

    private final UUID userId = UUID.randomUUID();
    private final User user = User.builder().id(userId).name("Test User").email("test@example.com").build();

    @Test
    void dashboard_ShouldReturnDashboardView() throws Exception {
        when(userService.getByEmail("test@example.com")).thenReturn(user);
        when(transactionService.getTotalAmount(any(), any(), any(), any())).thenReturn(BigDecimal.ZERO);
        when(transactionService.getTransactionsForUser(any(), any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attributeExists("income", "expense", "balance", "transactions"));
    }

    @Test
    void transactions_ShouldReturnTransactionsView() throws Exception {
        when(userService.getByEmail("test@example.com")).thenReturn(user);
        when(transactionService.getAllTransactionsForUser(userId)).thenReturn(List.of());
        when(categoryService.getCategoriesByUser(userId)).thenReturn(List.of());

        mockMvc.perform(get("/transactions"))
                .andExpect(status().isOk())
                .andExpect(view().name("transactions"))
                .andExpect(model().attributeExists("transactions", "categories", "newTransaction"));
    }

    @Test
    void addTransaction_Success_ShouldRedirect() throws Exception {
        when(userService.getByEmail("test@example.com")).thenReturn(user);
        when(transactionService.createTransaction(any(TransactionDto.class)))
                .thenReturn(Transaction.builder().id(UUID.randomUUID()).build());

        mockMvc.perform(post("/transactions")
                        .param("date", LocalDateTime.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                        .param("amount", "100")
                        .param("type", "INCOME")
                        .param("description", "Test transaction")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/transactions"));
    }

    @Test
    void addTransaction_ValidationException_ShouldReturnTransactionsViewWithError() throws Exception {
        when(userService.getByEmail("test@example.com")).thenReturn(user);
        when(transactionService.getAllTransactionsForUser(userId)).thenReturn(List.of());
        when(categoryService.getCategoriesByUser(userId)).thenReturn(List.of());

        doThrow(ValidationException.emptyDescription())
                .when(transactionService).createTransaction(any(TransactionDto.class));

        mockMvc.perform(post("/transactions")
                        .param("date", LocalDateTime.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                        .param("amount", "100")
                        .param("type", "INCOME")
                        .param("description", "")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("transactions"))
                .andExpect(model().attributeExists("error"))
                .andExpect(model().attribute("error", "Описание не может быть пустым"));
    }

    @Test
    void addTransaction_GenericException_ShouldReturnTransactionsViewWithError() throws Exception {
        when(userService.getByEmail("test@example.com")).thenReturn(user);
        when(transactionService.getAllTransactionsForUser(userId)).thenReturn(List.of());
        when(categoryService.getCategoriesByUser(userId)).thenReturn(List.of());

        doThrow(new RuntimeException("Database error"))
                .when(transactionService).createTransaction(any(TransactionDto.class));

        mockMvc.perform(post("/transactions")
                        .param("date", "")
                        .param("amount", "100")
                        .param("type", "INCOME")
                        .param("description", "Test")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("transactions"))
                .andExpect(model().attributeExists("error"))
                .andExpect(model().attribute("error", "Ошибка при создании транзакции: Database error"));
    }

    @Test
    void reports_ShouldReturnReportsView() throws Exception {
        when(userService.getByEmail("test@example.com")).thenReturn(user);

        mockMvc.perform(get("/reports"))
                .andExpect(status().isOk())
                .andExpect(view().name("reports"))
                .andExpect(model().attribute("periods", ReportPeriod.values()))
                .andExpect(model().attribute("user", user));
    }

    @Test
    void categories_ShouldReturnCategoriesView() throws Exception {
        when(userService.getByEmail("test@example.com")).thenReturn(user);
        var incomeCategory = CategoryDto.builder().id(UUID.randomUUID()).name("Salary").type(INCOME).build();
        var expenseCategory = CategoryDto.builder().id(UUID.randomUUID()).name("Food").type(EXPENSE).build();
        when(categoryService.getCategoriesByUser(userId)).thenReturn(List.of(incomeCategory, expenseCategory));

        mockMvc.perform(get("/categories"))
                .andExpect(status().isOk())
                .andExpect(view().name("categories"))
                .andExpect(model().attributeExists("incomeCategories", "expenseCategories"));
    }

    @Test
    void profile_ShouldReturnProfileView() throws Exception {
        when(userService.getByEmail("test@example.com")).thenReturn(user);

        mockMvc.perform(get("/profile"))
                .andExpect(status().isOk())
                .andExpect(view().name("profile"))
                .andExpect(model().attribute("user", user));
    }

    @Test
    void updateProfile_Success_ShouldRedirectWithUpdatedParam() throws Exception {
        when(userService.getByEmail("test@example.com")).thenReturn(user);
        when(userService.updateUser(any(), anyString(), anyString())).thenReturn(user);
        when(userService.loadUserByUsername(anyString())).thenReturn(
                org.springframework.security.core.userdetails.User
                        .withUsername("new@example.com")
                        .password("pass")
                        .roles("USER")
                        .build()
        );

        mockMvc.perform(post("/profile")
                        .param("name", "New Name")
                        .param("email", "new@example.com")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile?updated"));
    }

    @Test
    void updateProfile_ValidationException_ShouldReturnProfileViewWithError() throws Exception {
        when(userService.getByEmail("test@example.com")).thenReturn(user);
        doThrow(ValidationException.emailAlreadyExists("taken@example.com"))
                .when(userService).updateUser(any(), anyString(), anyString());

        mockMvc.perform(post("/profile")
                        .param("name", "New Name")
                        .param("email", "taken@example.com")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("profile"))
                .andExpect(model().attributeExists("error"))
                .andExpect(model().attribute("error", "Пользователь с email 'taken@example.com' уже существует"))
                .andExpect(model().attribute("name", "New Name"))
                .andExpect(model().attribute("email", "taken@example.com"));
    }
}
