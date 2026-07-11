package ru.vtvhw.spring.finance.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.vtvhw.spring.finance.entity.Category;
import ru.vtvhw.spring.finance.enums.TransactionType;

import java.util.List;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
    List<Category> findByUserId(UUID userId);
    List<Category> findByUserIdAndType(UUID userId, TransactionType type);
    boolean existsByNameAndUserId(String name, UUID userId);
}
