package ru.vtvhw.spring.finance.controller.rest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ru.vtvhw.spring.finance.dto.TransactionDto;
import ru.vtvhw.spring.finance.mapper.TransactionMapper;
import ru.vtvhw.spring.finance.service.TransactionService;
import ru.vtvhw.spring.finance.service.UserService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static java.util.Objects.isNull;

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
        if (isNull(from)) from = LocalDateTime.now().minusMonths(1);
        if (isNull(to)) to = LocalDateTime.now();
        return transactionService.getTransactionsForUser(user.getId(), from, to);
    }

    /**
     * Добавление новой транзакции через AJAX (JSON).
     */
    @PostMapping
    public TransactionDto addTransaction(Authentication auth, @RequestBody TransactionDto dto) {
        var user = userService.getByEmail(auth.getName());
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
                                            @RequestBody TransactionDto dto) {
        var user = userService.getByEmail(auth.getName());
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
