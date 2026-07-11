package ru.vtvhw.spring.finance.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.vtvhw.spring.finance.entity.User;
import ru.vtvhw.spring.finance.repository.UserRepository;
import ru.vtvhw.spring.finance.service.UserService;

import java.util.Optional;
import java.util.UUID;

import static java.lang.String.format;
import static org.apache.logging.log4j.util.Strings.isBlank;
import static ru.vtvhw.spring.finance.exception.ResourceNotFoundException.userNotFound;
import static ru.vtvhw.spring.finance.exception.ValidationException.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.error("User not found by email: {}", email);
                    return new UsernameNotFoundException(format("Пользователь не найден: %s", email));
                });
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword())
                .roles("USER")
                .build();
    }

    @Override
    @Transactional
    public User createUser(String name, String email, String rawPassword) {
        var user = User.builder()
                .name(name)
                .email(email)
                .password(passwordEncoder.encode(rawPassword))
                .build();
        var saved = userRepository.save(user);
        log.info("Created new user with id: {}", saved.getId());
        return saved;
    }

    @Override
    @Transactional
    public User updateUser(UUID id, String newName, String newEmail) {
        var user = getById(id);
        if (isBlank(newName)) {
            throw emptyUserName();
        }
        if (isBlank(newEmail)) {
            throw emptyEmail();
        }
        var existing = userRepository.findByEmail(newEmail);
        if (existing.isPresent() && !existing.get().getId().equals(id)) {
            throw emailAlreadyExists(newEmail);
        }
        user.setName(newName);
        user.setEmail(newEmail);
        var updated = userRepository.save(user);
        log.info("User updated: id={}, newName={}, newEmail={}", updated.getId(), updated.getName(), updated.getEmail());
        return updated;
    }

    @Override
    public User getById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("User not found by id: {}", id);
                    return userNotFound(id);
                });
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public User getByEmail(String email) {
        return findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("User not found by email: {}", email);
                    return userNotFound(email);
                });
    }

}
