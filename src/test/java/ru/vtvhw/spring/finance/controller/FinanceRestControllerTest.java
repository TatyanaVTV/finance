package ru.vtvhw.spring.finance.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.vtvhw.spring.finance.service.TransactionService;
import ru.vtvhw.spring.finance.service.UserService;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FinanceRestController.class)
public class FinanceRestControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private TransactionService transactionService;
    @MockitoBean private UserService userService;

    @Test
    @WithMockUser
    public void getTransactions_ShouldReturnOk() throws Exception {
        when(transactionService.getTransactionsForUser(any(), any(), any())).thenReturn(Collections.emptyList());
        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isOk());
    }
}
