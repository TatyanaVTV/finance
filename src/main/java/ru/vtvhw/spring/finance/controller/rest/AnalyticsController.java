package ru.vtvhw.spring.finance.controller.rest;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.vtvhw.spring.finance.service.TransactionService;
import ru.vtvhw.spring.finance.service.UserService;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;

import static ru.vtvhw.spring.finance.enums.TransactionType.EXPENSE;
import static ru.vtvhw.spring.finance.enums.TransactionType.INCOME;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {
    private final UserService userService;
    private final TransactionService transactionService;

    /**
     * Получение ключевых метрик (доходы, расходы, баланс) для динамического обновления дашборда.
     * Используется для AJAX-подгрузки на главной странице.
     */
    @GetMapping("/metrics")
    public Map<String, Object> getMetrics(Authentication auth) {
        var user = userService.getByEmail(auth.getName());
        var today = LocalDate.now();
        var startOfMonth = today.withDayOfMonth(1).atStartOfDay(); // от 00:00
        var endOfMonth = today.withDayOfMonth(today.lengthOfMonth()).atTime(LocalTime.MAX); // до 23:59

        var income = transactionService.getTotalAmount(user.getId(), INCOME, startOfMonth, endOfMonth);
        var expense = transactionService.getTotalAmount(user.getId(), EXPENSE, startOfMonth, endOfMonth);
        return Map.of(
                "income", income,
                "expense", expense,
                "balance", income.add(expense)
        );
    }
}
