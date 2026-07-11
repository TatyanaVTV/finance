package ru.vtvhw.spring.finance.controller.rest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.vtvhw.spring.finance.dto.TransactionDto;
import ru.vtvhw.spring.finance.entity.Transaction;
import ru.vtvhw.spring.finance.entity.User;
import ru.vtvhw.spring.finance.exception.ResourceNotFoundException;
import ru.vtvhw.spring.finance.exception.ValidationException;
import ru.vtvhw.spring.finance.mapper.TransactionMapper;
import ru.vtvhw.spring.finance.service.TransactionService;
import ru.vtvhw.spring.finance.service.UserService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static java.math.BigDecimal.TEN;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.vtvhw.spring.finance.enums.TransactionType.INCOME;
import static ru.vtvhw.spring.finance.exception.FinanceSecurityException.transactionNotBelongToUser;
import static ru.vtvhw.spring.finance.exception.ResourceNotFoundException.transactionNotFound;
import static ru.vtvhw.spring.finance.exception.ValidationException.emptyDescription;

@WebMvcTest(TransactionController.class)
@WithMockUser(username = "test@example.com")
public class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionService transactionService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private TransactionMapper transactionMapper;

    private UUID userId;
    private UUID transactionId;
    private User user;
    private Transaction transaction;
    private TransactionDto transactionDto;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        transactionId = UUID.randomUUID();
        user = User.builder().id(userId).name("Test User").email("test@example.com").build();
        transaction = Transaction.builder()
                .id(transactionId)
                .user(user)
                .amount(TEN)
                .type(INCOME)
                .date(LocalDateTime.now())
                .description("Test")
                .build();

        transactionDto = new TransactionDto();
        transactionDto.setId(transactionId);
        transactionDto.setUserId(userId);
        transactionDto.setAmount(TEN);
        transactionDto.setType(INCOME);
        transactionDto.setDate(LocalDateTime.now());
        transactionDto.setDescription("Test");
    }

    @Test
    void getTransactions_WithoutParams_ShouldUseDefaultPeriod() throws Exception {
        when(userService.getByEmail("test@example.com")).thenReturn(user);
        when(transactionService.getTransactionsForUser(eq(userId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(transactionDto));

        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(transactionId.toString()))
                .andExpect(jsonPath("$[0].amount").value(10));
    }

    @Test
    void getTransactions_WithParams_ShouldReturnFiltered() throws Exception {
        when(userService.getByEmail("test@example.com")).thenReturn(user);
        var from = LocalDateTime.now().minusDays(10);
        var to = LocalDateTime.now();
        when(transactionService.getTransactionsForUser(eq(userId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(transactionDto));

        mockMvc.perform(get("/api/transactions")
                        .param("from", from.toString())
                        .param("to", to.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(transactionId.toString()));
    }

    @Test
    void addTransaction_Success() throws Exception {
        when(userService.getByEmail("test@example.com")).thenReturn(user);
        when(transactionService.createTransaction(any(TransactionDto.class))).thenReturn(transaction);
        when(transactionMapper.toDto(transaction)).thenReturn(transactionDto);

        var json = """
                {
                    "amount": 10,
                    "type": "INCOME",
                    "description": "Test"
                }
                """;

        mockMvc.perform(post("/api/transactions")
                        .contentType(APPLICATION_JSON)
                        .content(json)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(transactionId.toString()))
                .andExpect(jsonPath("$.amount").value(10));
    }

    @Test
    void addTransaction_ValidationException_ShouldReturnBadRequest() throws Exception {
        when(userService.getByEmail("test@example.com")).thenReturn(user);
        doThrow(emptyDescription())
                .when(transactionService).createTransaction(any(TransactionDto.class));

        var json = """
                {
                    "amount": 10,
                    "type": "INCOME",
                    "description": ""
                }
                """;

        mockMvc.perform(post("/api/transactions")
                        .contentType(APPLICATION_JSON)
                        .content(json)
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Описание не может быть пустым"));
    }

    @Test
    void updateTransaction_Success() throws Exception {
        when(userService.getByEmail("test@example.com")).thenReturn(user);
        when(transactionService.updateTransaction(eq(transactionId), any(TransactionDto.class))).thenReturn(transaction);
        when(transactionMapper.toDto(transaction)).thenReturn(transactionDto);

        var json = """
                {
                    "amount": 20,
                    "type": "INCOME",
                    "description": "Updated"
                }
                """;

        mockMvc.perform(put("/api/transactions/{id}", transactionId)
                        .contentType(APPLICATION_JSON)
                        .content(json)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(transactionId.toString()))
                .andExpect(jsonPath("$.amount").value(10));
    }

    @Test
    void updateTransaction_ValidationException_ShouldReturnBadRequest() throws Exception {
        when(userService.getByEmail("test@example.com")).thenReturn(user);
        doThrow(ValidationException.invalidAmount())
                .when(transactionService).updateTransaction(eq(transactionId), any(TransactionDto.class));

        var json = """
                {
                    "amount": -5,
                    "type": "INCOME",
                    "description": "Test"
                }
                """;

        mockMvc.perform(put("/api/transactions/{id}", transactionId)
                        .contentType(APPLICATION_JSON)
                        .content(json)
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Сумма должна быть положительным числом"));
    }

    @Test
    void updateTransaction_ResourceNotFound_ShouldReturnNotFound() throws Exception {
        when(userService.getByEmail("test@example.com")).thenReturn(user);
        doThrow(transactionNotFound(transactionId))
                .when(transactionService).updateTransaction(eq(transactionId), any(TransactionDto.class));

        var json = """
                {
                    "amount": 20,
                    "type": "INCOME",
                    "description": "Test"
                }
                """;

        mockMvc.perform(put("/api/transactions/{id}", transactionId)
                        .contentType(APPLICATION_JSON)
                        .content(json)
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Транзакция не найдена: " + transactionId));
    }

    @Test
    void updateTransaction_SecurityException_ShouldReturnForbidden() throws Exception {
        when(userService.getByEmail("test@example.com")).thenReturn(user);
        doThrow(transactionNotBelongToUser(transactionId, userId))
                .when(transactionService).updateTransaction(eq(transactionId), any(TransactionDto.class));

        var json = """
                {
                    "amount": 20,
                    "type": "INCOME",
                    "description": "Test"
                }
                """;

        mockMvc.perform(put("/api/transactions/{id}", transactionId)
                        .contentType(APPLICATION_JSON)
                        .content(json)
                        .with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Транзакция '" + transactionId + "' не принадлежит указанному пользователю '" + userId + "'"));
    }

    @Test
    void deleteTransaction_Success() throws Exception {
        when(userService.getByEmail("test@example.com")).thenReturn(user);

        mockMvc.perform(delete("/api/transactions/{id}", transactionId)
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteTransaction_ResourceNotFound_ShouldReturnNotFound() throws Exception {
        when(userService.getByEmail("test@example.com")).thenReturn(user);
        doThrow(transactionNotFound(transactionId))
                .when(transactionService).deleteTransaction(transactionId, userId);

        mockMvc.perform(delete("/api/transactions/{id}", transactionId)
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Транзакция не найдена: " + transactionId));
    }

    @Test
    void deleteTransaction_SecurityException_ShouldReturnForbidden() throws Exception {
        when(userService.getByEmail("test@example.com")).thenReturn(user);
        doThrow(transactionNotBelongToUser(transactionId, userId))
                .when(transactionService).deleteTransaction(transactionId, userId);

        mockMvc.perform(delete("/api/transactions/{id}", transactionId)
                        .with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Транзакция '" + transactionId + "' не принадлежит указанному пользователю '" + userId + "'"));
    }

    @Test
    void getTransactionById_Success() throws Exception {
        when(userService.getByEmail("test@example.com")).thenReturn(user);
        when(transactionService.getTransactionById(transactionId, userId)).thenReturn(transactionDto);

        mockMvc.perform(get("/api/transactions/{id}", transactionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(transactionId.toString()))
                .andExpect(jsonPath("$.amount").value(10));
    }

    @Test
    void getTransactionById_ResourceNotFound_ShouldReturnNotFound() throws Exception {
        when(userService.getByEmail("test@example.com")).thenReturn(user);
        doThrow(ResourceNotFoundException.transactionNotFound(transactionId))
                .when(transactionService).getTransactionById(transactionId, userId);

        mockMvc.perform(get("/api/transactions/{id}", transactionId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Транзакция не найдена: " + transactionId));
    }

    @Test
    void getTransactionById_SecurityException_ShouldReturnForbidden() throws Exception {
        when(userService.getByEmail("test@example.com")).thenReturn(user);
        doThrow(transactionNotBelongToUser(transactionId, userId))
                .when(transactionService).getTransactionById(transactionId, userId);

        mockMvc.perform(get("/api/transactions/{id}", transactionId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Транзакция '" + transactionId + "' не принадлежит указанному пользователю '" + userId + "'"));
    }
}
