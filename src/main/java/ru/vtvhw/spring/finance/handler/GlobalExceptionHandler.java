package ru.vtvhw.spring.finance.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.NoHandlerFoundException;
import ru.vtvhw.spring.finance.exception.FinanceException;

import java.time.LocalDateTime;

import static org.springframework.http.HttpStatus.*;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(FinanceException.class)
    @ResponseStatus(BAD_REQUEST)
    public String handleFinanceException(FinanceException ex, Model model) {
        log.error("Finance exception: {}", ex.getMessage());
        model.addAttribute("error", ex.getMessage());
        model.addAttribute("status", BAD_REQUEST.value());
        model.addAttribute("timestamp", LocalDateTime.now());
        return "error";
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(INTERNAL_SERVER_ERROR)
    public String handleGenericException(Exception ex, Model model) {
        log.error("Unexpected error", ex);
        model.addAttribute("error", "Внутренняя ошибка сервера");
        model.addAttribute("status", INTERNAL_SERVER_ERROR.value());
        model.addAttribute("timestamp", LocalDateTime.now());
        return "error";
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(NOT_FOUND)
    public String handleNotFound(NoHandlerFoundException ex, Model model) {
        model.addAttribute("error", "Страница не найдена");
        model.addAttribute("status", NOT_FOUND.value());
        model.addAttribute("timestamp", LocalDateTime.now());
        return "error";
    }
}
