package ru.vtvhw.spring.finance.exception;

import java.util.UUID;

import static java.lang.String.format;

public class FinanceSecurityException extends SecurityException {
    private FinanceSecurityException(String message) {
        super(message);
    }

    public static FinanceSecurityException categoryNotBelongToUser(UUID categoryId, UUID userId) {
        return new FinanceSecurityException(
                format("Категория '%s' не принадлежит указанному пользователю '%s'", categoryId, userId)
        );
    }

    public static FinanceSecurityException transactionNotBelongToUser(UUID categoryId, UUID userId) {
        return new FinanceSecurityException(
                format("Транзакция '%s' не принадлежит указанному пользователю '%s'", categoryId, userId)
        );
    }
}
