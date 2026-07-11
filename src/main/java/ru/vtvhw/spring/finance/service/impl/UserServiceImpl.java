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
import ru.vtvhw.spring.finance.service.CategoryService;
import ru.vtvhw.spring.finance.service.UserService;

import java.util.Optional;
import java.util.UUID;

import static java.lang.String.format;
import static ru.vtvhw.spring.finance.enums.TransactionType.EXPENSE;
import static ru.vtvhw.spring.finance.enums.TransactionType.INCOME;
import static ru.vtvhw.spring.finance.exception.ResourceNotFoundException.userNotFound;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CategoryService categoryService;

    private static final String[] DEFAULT_INCOME_CATEGORIES = {"Зарплата", "Фриланс", "Подарки", "Инвестиции"};
    private static final String[] DEFAULT_EXPENSE_CATEGORIES = {"Продукты", "Транспорт", "Коммунальные", "Развлечения",
            "Здоровье", "Одежда", "Образование", "Другое"};

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

        createDefaultCategories(saved);
        return saved;
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

    private void createDefaultCategories(User user) {
        for (String catName : DEFAULT_INCOME_CATEGORIES) {
            categoryService.createCategory(catName, INCOME, user.getId());
        }
        for (String catName : DEFAULT_EXPENSE_CATEGORIES) {
            categoryService.createCategory(catName, EXPENSE, user.getId());
        }
        log.info("Default categories created for user {}", user.getId());
    }

}
