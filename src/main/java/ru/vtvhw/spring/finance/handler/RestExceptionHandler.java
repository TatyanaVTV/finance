package ru.vtvhw.spring.finance.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import ru.vtvhw.spring.finance.exception.*;

import java.util.Map;

import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;
import static org.springframework.http.HttpStatus.*;

@Order(HIGHEST_PRECEDENCE)
@ControllerAdvice(annotations = RestController.class)
@Slf4j
public class RestExceptionHandler {

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Map<String, String>> handleValidation(ValidationException e) {
        log.warn("Validation error: {}", e.getMessage());
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(ExportException.class)
    public ResponseEntity<Map<String, String>> handleExport(ExportException e) {
        log.warn("Export error: {}", e.getMessage());
        return ResponseEntity.status(NOT_IMPLEMENTED).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(FinanceSecurityException.class)
    public ResponseEntity<Map<String, String>> handleSecurity(FinanceSecurityException e) {
        log.warn("Security error: {}", e.getMessage());
        return ResponseEntity.status(FORBIDDEN).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(ResourceNotFoundException e) {
        log.warn("Resource not found: {}", e.getMessage());
        return ResponseEntity.status(NOT_FOUND).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(FinanceException.class)
    public ResponseEntity<Map<String, String>> handleFinanceException(FinanceException ex) {
        log.error("Finance exception: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception ex) {
        log.error("REST unexpected error", ex);
        return ResponseEntity.internalServerError().body(Map.of("error", "Внутренняя ошибка сервера"));
    }
}
