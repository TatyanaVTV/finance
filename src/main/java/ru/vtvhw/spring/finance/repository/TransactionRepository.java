package ru.vtvhw.spring.finance.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.vtvhw.spring.finance.entity.Transaction;
import ru.vtvhw.spring.finance.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    List<Transaction> findByUserIdOrderByDateDesc(UUID userId);
    List<Transaction> findByUserIdAndDateBetweenOrderByDateDesc(UUID userId, LocalDateTime from, LocalDateTime to);

    @EntityGraph(attributePaths = "category") // join fetch
    Page<Transaction> findByUserId(UUID userId, Pageable pageable);

    @EntityGraph(attributePaths = "category")
    Page<Transaction> findByUserIdAndDateBetween(
            UUID userId, LocalDateTime from, LocalDateTime to, Pageable pageable);

    boolean existsByCategoryId(UUID categoryId);

    @Query("""
        SELECT SUM(t.amount)
        FROM Transaction t
        WHERE t.user.id = :userId AND t.type = :type AND t.date BETWEEN :from AND :to
    """)
    BigDecimal sumAmountByUserAndTypeAndDateRange(@Param("userId") UUID userId,
                                                  @Param("type") TransactionType type,
                                                  @Param("from") LocalDateTime from,
                                                  @Param("to") LocalDateTime to);
}
