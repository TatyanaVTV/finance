package ru.vtvhw.spring.finance.service;

import org.springframework.security.core.userdetails.UserDetailsService;
import ru.vtvhw.spring.finance.entity.User;

import java.util.Optional;
import java.util.UUID;

public interface UserService extends UserDetailsService {
    User createUser(String name, String email, String rawPassword);
    User getById(UUID id);
    Optional<User> findByEmail(String email);
    User getByEmail(String email);
}
