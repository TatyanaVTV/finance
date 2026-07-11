package ru.vtvhw.spring.finance.exception;

import ru.vtvhw.spring.finance.enums.TransactionType;

import static java.lang.String.format;

public class ValidationException extends RuntimeException {
    private ValidationException(String message) {
        super(message);
    }

    private ValidationException(String message, Throwable cause) {
        super(message, cause);
    }

    public static ValidationException invalidAmount() {
        return new ValidationException("Сумма должна быть положительным числом");
    }

    public static ValidationException invalidDateRange() {
        return new ValidationException("Дата начала не может быть позже даты окончания");
    }

    public static ValidationException emptyDescription() {
        return new ValidationException("Описание не может быть пустым");
    }

    public static ValidationException categoryAlreadyExists(String name) {
        return new ValidationException(format("Категория с именем '%s' уже существует", name));
    }

    public static ValidationException categoryTypeMismatch(TransactionType transactionType, TransactionType categoryType) {
        return new ValidationException(
                String.format("Тип категории (%s) не соответствует типу транзакции (%s)", categoryType, transactionType)
        );
    }
}
