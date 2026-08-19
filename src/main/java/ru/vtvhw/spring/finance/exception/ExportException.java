package ru.vtvhw.spring.finance.exception;

import ru.vtvhw.spring.finance.enums.ExportFormat;

import static java.lang.String.format;

public class ExportException extends RuntimeException {
    private ExportException(String message) {
        super(message);
    }

    public static ExportException notSupported(ExportFormat exportFormat) {
        return new ExportException(format("Формат '%s' не поддерживается", exportFormat));
    }
}
