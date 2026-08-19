package ru.vtvhw.spring.finance.exception;

public abstract class FinanceException extends RuntimeException {
    protected FinanceException(String message) {
        super(message);
    }
}
