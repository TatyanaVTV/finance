package ru.vtvhw.spring.finance.exception;

import ru.vtvhw.spring.finance.enums.TransactionType;

import java.util.UUID;

import static java.lang.String.format;

public class ValidationException extends RuntimeException {
    private ValidationException(String message) {
        super(message);
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

    public static ValidationException categoryHasTransactions(UUID categoryId) {
        return new ValidationException(
                String.format("Невозможно удалить категорию (ID: %s), так как существуют транзакции, ссылающиеся на неё. Сначала переназначьте или удалите эти транзакции.", categoryId)
        );
    }

    public static ValidationException emptyCategoryName() {
        return new ValidationException("Название категории не может быть пустым");
    }

    public static ValidationException emptyUserName() {
        return new ValidationException("Имя не может быть пустым");
    }
    public static ValidationException emptyEmail() {
        return new ValidationException("Email не может быть пустым");
    }
    public static ValidationException emailAlreadyExists(String email) {
        return new ValidationException(format("Пользователь с email '%s' уже существует", email));
    }
}
