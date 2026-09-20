package ru.vtvhw.spring.finance.controller.rest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ru.vtvhw.spring.finance.dto.transaction.CreateTransactionRequest;
import ru.vtvhw.spring.finance.dto.transaction.TransactionDto;
import ru.vtvhw.spring.finance.dto.transaction.UpdateTransactionRequest;
import ru.vtvhw.spring.finance.mapper.TransactionMapper;
import ru.vtvhw.spring.finance.service.TransactionService;
import ru.vtvhw.spring.finance.service.UserService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Slf4j
public class TransactionController {

    private final TransactionService transactionService;
    private final UserService userService;
    private final TransactionMapper transactionMapper;

    /**
     * Получение транзакций за период (по умолчанию – за последний месяц).
     * Используется для AJAX-обновления списка на страницах.
     */
    @GetMapping
    public List<TransactionDto> getTransactions(Authentication auth,
                                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
                                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        var user = userService.getByEmail(auth.getName());
        return transactionService.getTransactionsForUser(user.getId(), from, to);
    }

    /**
     * Добавление новой транзакции через AJAX (JSON).
     */
    @PostMapping
    public TransactionDto addTransaction(Authentication auth,
                                         @Valid @RequestBody CreateTransactionRequest request) {
        var user = userService.getByEmail(auth.getName());
        var dto = transactionMapper.toDto(request);
        dto.setUserId(user.getId());
        var created = transactionService.createTransaction(dto);
        return transactionMapper.toDto(created);
    }

    /**
     * Обновление транзакции
     */
    @PutMapping("/{id}")
    public TransactionDto updateTransaction(Authentication auth,
                                            @PathVariable UUID id,
                                            @Valid @RequestBody UpdateTransactionRequest request) {
        var user = userService.getByEmail(auth.getName());
        var dto = transactionMapper.toDto(request);
        dto.setUserId(user.getId());
        var updated = transactionService.updateTransaction(id, dto);
        return transactionMapper.toDto(updated);
    }

    /**
     * Удаление транзакции
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransaction(Authentication auth,
                                                  @PathVariable UUID id) {
        var user = userService.getByEmail(auth.getName());
        transactionService.deleteTransaction(id, user.getId());
        return ResponseEntity.noContent().build();
    }

    /**
     * Получение одной транзакции по id
     */
    @GetMapping("/{id}")
    public TransactionDto getTransactionById(Authentication auth, @PathVariable UUID id) {
        var user = userService.getByEmail(auth.getName());
        return transactionService.getTransactionById(id, user.getId());
    }
}
