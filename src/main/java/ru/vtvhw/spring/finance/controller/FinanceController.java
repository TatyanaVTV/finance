package ru.vtvhw.spring.finance.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.vtvhw.spring.finance.dto.CategoryDto;
import ru.vtvhw.spring.finance.dto.TransactionDto;
import ru.vtvhw.spring.finance.enums.ReportPeriod;
import ru.vtvhw.spring.finance.exception.ValidationException;
import ru.vtvhw.spring.finance.service.CategoryService;
import ru.vtvhw.spring.finance.service.TransactionService;
import ru.vtvhw.spring.finance.service.UserService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;
import static java.util.Objects.isNull;
import static org.apache.logging.log4j.util.Strings.isBlank;
import static ru.vtvhw.spring.finance.enums.TransactionType.EXPENSE;
import static ru.vtvhw.spring.finance.enums.TransactionType.INCOME;

@Controller
@RequiredArgsConstructor
@Slf4j
public class FinanceController {

    private final TransactionService transactionService;
    private final CategoryService categoryService;
    private final UserService userService;

    /**
     * Главная страница дашборда.
     * Отображает метрики за текущий месяц и список последних транзакций.
     */
    @GetMapping("/dashboard")
    public String dashboard(Authentication auth, Model model) {
        var user = userService.getByEmail(auth.getName());
        var now = LocalDateTime.now();
        var startOfMonth = now.withDayOfMonth(1);
        var endOfMonth = now.withDayOfMonth(now.toLocalDate().lengthOfMonth());

        var income = transactionService.getTotalAmount(user.getId(), INCOME, startOfMonth, endOfMonth);
        var expense = transactionService.getTotalAmount(user.getId(), EXPENSE, startOfMonth, endOfMonth);
        var balance = income.subtract(expense);
        var transactions = transactionService.getTransactionsForUser(user.getId(), startOfMonth, endOfMonth);

        model.addAttribute("income", income);
        model.addAttribute("expense", expense);
        model.addAttribute("balance", balance);
        model.addAttribute("transactions", transactions);
        return "dashboard";
    }

    /**
     * Страница управления транзакциями.
     * Показывает список всех транзакций пользователя и форму для добавления новой.
     */
    @GetMapping("/transactions")
    public String transactions(Authentication auth, Model model) {
        var user = userService.getByEmail(auth.getName());
        model.addAttribute("transactions", transactionService.getAllTransactionsForUser(user.getId()));
        model.addAttribute("categories", categoryService.getCategoriesByUser(user.getId()));
        model.addAttribute("newTransaction", new TransactionDto());
        return "transactions";
    }

    /**
     * Обработка формы добавления транзакции.
     */
    @PostMapping("/transactions")
    public String addTransaction(Authentication auth,
                                 @RequestParam(required = false) String date,
                                 @ModelAttribute("newTransaction") TransactionDto dto, Model model) {
        var user = userService.getByEmail(auth.getName());
        dto.setUserId(user.getId());
        dto.setDate(formatDate(date));

        try {
            transactionService.createTransaction(dto);
            return "redirect:/transactions";
        } catch (ValidationException e) {
            var transactions = transactionService.getAllTransactionsForUser(user.getId());
            var categories = categoryService.getCategoriesByUser(user.getId());
            prepareCreationErrorModel(model, e.getMessage(), transactions, categories, dto);
            return "transactions";
        } catch (Exception e) {
            var transactions = transactionService.getAllTransactionsForUser(user.getId());
            var categories = categoryService.getCategoriesByUser(user.getId());
            prepareCreationErrorModel(model, "Ошибка при создании транзакции: " + e.getMessage(), transactions, categories, dto);
            return "transactions";
        }
    }

    /**
     * Страница генерации отчётов с выбором периода и формата.
     */
    @GetMapping("/reports")
    public String reports(Authentication auth, Model model) {
        var user = userService.getByEmail(auth.getName());
        model.addAttribute("periods", ReportPeriod.values());
        model.addAttribute("user", user);
        return "reports";
    }

    /**
     * Страница управления категориями.
     */
    @GetMapping("/categories")
    public String categories(Authentication auth, Model model) {
        var user = userService.getByEmail(auth.getName());
        model.addAttribute("categories", categoryService.getCategoriesByUser(user.getId()));
        return "categories";
    }

    /**
     * Страница профиля пользователя.
     */
    @GetMapping("/profile")
    public String profile(Authentication auth, Model model) {
        var user = userService.getByEmail(auth.getName());
        model.addAttribute("user", user);
        return "profile";
    }

    private void prepareCreationErrorModel(Model model, String errorMessage,
                                           List<TransactionDto> transactions,
                                           List<CategoryDto> categories,
                                           TransactionDto dto) {
        var formattedDate = isNull(dto.getDate()) ? "" : dto.getDate().format(ISO_LOCAL_DATE_TIME);
        model.addAttribute("error", errorMessage);
        model.addAttribute("transactions", transactions);
        model.addAttribute("categories", categories);
        model.addAttribute("newTransaction", dto);
        model.addAttribute("formattedDate", formattedDate);
    }

    private LocalDateTime formatDate(String date) {
        var formattedDate = LocalDateTime.now();

        if (!isBlank(date)) {
            try {
                formattedDate = LocalDateTime.parse(date, ISO_LOCAL_DATE_TIME);
            } catch (Exception ignore) {}
        }

        return formattedDate;
    }
}
