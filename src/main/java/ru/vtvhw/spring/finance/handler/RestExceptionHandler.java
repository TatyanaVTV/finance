package ru.vtvhw.spring.finance.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import ru.vtvhw.spring.finance.exception.*;

import java.util.LinkedHashMap;
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

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        var fields = new LinkedHashMap<String, String>();

        ex.getBindingResult().getFieldErrors().forEach(err ->
                fields.putIfAbsent(err.getField(), err.getDefaultMessage()));

        // Классовые constraint-ошибки
        ex.getBindingResult().getGlobalErrors().forEach(err ->
                fields.putIfAbsent("form", err.getDefaultMessage()));

        log.warn("REST validation error: {}", fields);
        return ResponseEntity.badRequest().body(Map.of(
                "error", "Некорректные параметры запроса",
                "fields", fields
        ));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, String>> handleMissingParam(MissingServletRequestParameterException ex) {
        log.warn("Missing request parameter: {}", ex.getParameterName());
        return ResponseEntity.badRequest().body(Map.of(
                "error", "Отсутствует обязательный параметр: " + ex.getParameterName()
        ));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.warn("Invalid value for parameter {}: {}", ex.getName(), ex.getValue());
        var message = "Некорректное значение параметра '" + ex.getName() + "': " + ex.getValue();
        return ResponseEntity.badRequest().body(Map.of(
                "error", "Некорректные параметры запроса",
                "fields", Map.of(ex.getName(), message)
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception ex) {
        log.error("REST unexpected error", ex);
        return ResponseEntity.internalServerError().body(Map.of("error", "Внутренняя ошибка сервера"));
    }
}
