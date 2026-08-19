package ru.vtvhw.spring.finance.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.vtvhw.spring.finance.entity.User;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository  extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
}
