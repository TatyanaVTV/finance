package ru.vtvhw.spring.finance.service;

import ru.vtvhw.spring.finance.dto.TransactionDto;
import ru.vtvhw.spring.finance.entity.Transaction;
import ru.vtvhw.spring.finance.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface TransactionService {
    Transaction createTransaction(TransactionDto dto);
    Transaction updateTransaction(UUID id, TransactionDto dto);
    void deleteTransaction(UUID id, UUID userId);
    List<TransactionDto> getTransactionsForUser(UUID userId, LocalDateTime from, LocalDateTime to);
    BigDecimal getTotalAmount(UUID userId, TransactionType type, LocalDateTime from, LocalDateTime to);
    List<TransactionDto> getAllTransactionsForUser(UUID userId);
    TransactionDto getTransactionById(UUID id, UUID userId);
}
