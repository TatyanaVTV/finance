package ru.vtvhw.spring.finance.exception;

import java.util.UUID;

import static java.lang.String.format;

public class ResourceNotFoundException extends FinanceException {
    private ResourceNotFoundException(String message) {
        super(message);
    }

    public static ResourceNotFoundException userNotFound(UUID id) {
        return new ResourceNotFoundException(format("Пользователь не найден: %s", id));
    }

    public static ResourceNotFoundException userNotFound(String email) {
        return new ResourceNotFoundException(format("Пользователь не найден: %s", email));
    }

    public static ResourceNotFoundException transactionNotFound(UUID id) {
        return new ResourceNotFoundException(format("Транзакция не найдена: %s", id));
    }

    public static ResourceNotFoundException categoryNotFound(UUID id) {
        return new ResourceNotFoundException(format("Категория не найдена: %s", id));
    }

    public static ResourceNotFoundException reportNotFound(UUID id) {
        return new ResourceNotFoundException(format("Отчёт не найден: %s", id));
    }
}
