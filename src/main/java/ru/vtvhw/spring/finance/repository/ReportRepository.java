package ru.vtvhw.spring.finance.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.vtvhw.spring.finance.entity.Report;
import java.util.UUID;

public interface ReportRepository extends JpaRepository<Report, UUID> {
}
