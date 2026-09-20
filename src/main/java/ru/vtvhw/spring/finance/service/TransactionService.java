package ru.vtvhw.spring.finance.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.vtvhw.spring.finance.dto.transaction.TransactionDto;
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
    Page<TransactionDto> getTransactionsForUser(UUID userId, LocalDateTime from, LocalDateTime to, Pageable pageable);
    BigDecimal getTotalAmount(UUID userId, TransactionType type, LocalDateTime from, LocalDateTime to);
    Page<TransactionDto> getAllTransactionsForUser(UUID userId, Pageable pageable);
    TransactionDto getTransactionById(UUID id, UUID userId);
}
