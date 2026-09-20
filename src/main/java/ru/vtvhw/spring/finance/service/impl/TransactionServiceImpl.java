package ru.vtvhw.spring.finance.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.vtvhw.spring.finance.dto.transaction.TransactionDto;
import ru.vtvhw.spring.finance.entity.Category;
import ru.vtvhw.spring.finance.entity.Transaction;
import ru.vtvhw.spring.finance.entity.User;
import ru.vtvhw.spring.finance.enums.TransactionType;
import ru.vtvhw.spring.finance.mapper.TransactionMapper;
import ru.vtvhw.spring.finance.repository.CategoryRepository;
import ru.vtvhw.spring.finance.repository.TransactionRepository;
import ru.vtvhw.spring.finance.repository.UserRepository;
import ru.vtvhw.spring.finance.service.TransactionService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.math.BigDecimal.ZERO;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.logging.log4j.util.Strings.isBlank;
import static ru.vtvhw.spring.finance.enums.TransactionType.EXPENSE;
import static ru.vtvhw.spring.finance.exception.FinanceSecurityException.categoryNotBelongToUser;
import static ru.vtvhw.spring.finance.exception.FinanceSecurityException.transactionNotBelongToUser;
import static ru.vtvhw.spring.finance.exception.ResourceNotFoundException.*;
import static ru.vtvhw.spring.finance.exception.ValidationException.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionServiceImpl implements TransactionService {
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionMapper transactionMapper;

    @Override
    @Transactional
    @CacheEvict(value = "totals", allEntries = true)
    public Transaction createTransaction(TransactionDto dto) {
        var user = getUserOrThrow(dto.getUserId());

        normalizeAndValidateAmount(dto);
        validateDescription(dto.getDescription());

        var category = getCategoryOrThrow(dto.getCategoryId(), user.getId(), dto.getType());

        var entity = transactionMapper.toEntity(dto);
        entity.setUser(user);
        entity.setCategory(category);
        setTransactionDate(entity, dto);

        var saved = transactionRepository.save(entity);
        log.info("Transaction created: id={}, amount={}, type={}, category={}, date={}",
                saved.getId(), saved.getAmount(), saved.getType(), saved.getCategory(), saved.getDate());
        return saved;
    }

    @Override
    @Transactional
    @CacheEvict(value = "totals", allEntries = true)
    public Transaction updateTransaction(UUID id, TransactionDto dto) {
        var existing = getTransactionOrThrow(id);
        validateTransactionOwnership(existing, dto.getUserId(), id);

        normalizeAndValidateAmount(dto);
        validateDescription(dto.getDescription());
        var category = getCategoryOrThrow(dto.getCategoryId(), existing.getUser().getId(), dto.getType());

        existing.setAmount(dto.getAmount());
        existing.setType(dto.getType());
        existing.setDescription(dto.getDescription());

        if (nonNull(dto.getDate())) {
            existing.setDate(dto.getDate());
        }
        existing.setCategory(category);

        var updated = transactionRepository.save(existing);
        log.info("Transaction updated: id={}", updated.getId());
        return updated;
    }

    @Override
    @Transactional
    @CacheEvict(value = "totals", allEntries = true)
    public void deleteTransaction(UUID id, UUID userId) {
        var transaction = getTransactionOrThrow(id);
        validateTransactionOwnership(transaction, userId, id);
        transactionRepository.delete(transaction);
        log.info("Transaction deleted: id={}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionDto> getTransactionsForUser(UUID userId, LocalDateTime from, LocalDateTime to) {
        from = normalizeFrom(from, to);
        to = normalizeTo(from, to);

        var transactions = transactionRepository.findByUserIdAndDateBetweenOrderByDateDesc(userId, from, to);
        log.info("Getting transactions for user: from={}, to={}", from, to);
        return transactions.stream()
                .map(transactionMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionDto> getTransactionsForUser(UUID userId, LocalDateTime from, LocalDateTime to,
                                                       Pageable pageable) {
        from = normalizeFrom(from, to);
        to = normalizeTo(from, to);

        log.info("Getting transactions for user: from={}, to={}, pageNb={}, pageSize={}",
                from, to, pageable.getPageNumber(), pageable.getPageSize());
        return transactionRepository.findByUserIdAndDateBetween(userId, from, to, pageable)
                .map(transactionMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionDto getTransactionById(UUID id, UUID userId) {
        var transaction = getTransactionOrThrow(id);
        validateTransactionOwnership(transaction, userId, id);
        return transactionMapper.toDto(transaction);
    }

    @Override
    @Cacheable(value = "totals", key = "#userId + ':' + #type + ':' + #from + ':' + #to")
    public BigDecimal getTotalAmount(UUID userId, TransactionType type, LocalDateTime from, LocalDateTime to) {
        log.info("Getting total amount for userId={}, type={}, from={}, to={}", userId, type, from, to);
        var sum = transactionRepository.sumAmountByUserAndTypeAndDateRange(userId, type, from, to);
        return sum != null ? sum : ZERO;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionDto> getAllTransactionsForUser(UUID userId, Pageable pageable) {
        log.info("Getting all transactions for userId={}, page#{}, page size={}",
                userId, pageable.getPageNumber(), pageable.getPageSize());
        return transactionRepository.findByUserId(userId, pageable)
                .map(transactionMapper::toDto);
    }

    private User getUserOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User not found: {}", userId);
                    return userNotFound(userId);
                });
    }

    private Category getCategoryOrThrow(UUID categoryId, UUID userId, TransactionType transactionType) {
        if (isNull(categoryId)) {
            return null;
        }
        var category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> {
                    log.warn("Category not found: {}", categoryId);
                    return categoryNotFound(categoryId);
                });
        if (!category.getUser().getId().equals(userId)) {
            throw categoryNotBelongToUser(categoryId, userId);
        }
        if (category.getType() != transactionType) {
            throw categoryTypeMismatch(transactionType, category.getType());
        }
        return category;
    }

    private BigDecimal normalizeAndValidateAmount(TransactionDto dto) {
        var amount = dto.getAmount();
        if (dto.getType() == EXPENSE) {
            if (amount.compareTo(ZERO) > 0) {
                amount = amount.negate();
                dto.setAmount(amount);
            }
        } else {
            if (amount.compareTo(ZERO) <= 0) {
                throw invalidAmount();
            }
        }
        return amount;
    }

    private LocalDateTime normalizeFrom(LocalDateTime from, LocalDateTime to) {
        if (isNull(from) && isNull(to)) {
            return LocalDateTime.now().minusMonths(1);
        }
        if (isNull(from)) {
            return to.minusMonths(1);
        }
        if (!isNull(to) && from.isAfter(to)) {
            throw invalidDateRange();
        }
        return from;
    }

    private LocalDateTime normalizeTo(LocalDateTime from, LocalDateTime to) {
        if (isNull(from) && isNull(to)) {
            return LocalDateTime.now();
        }
        if (isNull(to)) {
            return from.plusMonths(1);
        }
        return to;
    }

    private void validateDescription(String description) {
        if (isBlank(description)) {
            throw emptyDescription();
        }
    }

    private void setTransactionDate(Transaction entity, TransactionDto dto) {
        entity.setDate(nonNull(dto.getDate()) ? dto.getDate() : LocalDateTime.now());
    }

    private void validateTransactionOwnership(Transaction transaction, UUID userId, UUID transactionId) {
        if (!transaction.getUser().getId().equals(userId)) {
            throw transactionNotBelongToUser(transactionId, userId);
        }
    }

    private Transaction getTransactionOrThrow(UUID id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Transaction not found: {}", id);
                    return transactionNotFound(id);
                });
    }
}