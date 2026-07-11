package ru.vtvhw.spring.finance.init;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.vtvhw.spring.finance.dto.TransactionDto;
import ru.vtvhw.spring.finance.entity.User;
import ru.vtvhw.spring.finance.service.CategoryService;
import ru.vtvhw.spring.finance.service.TransactionService;
import ru.vtvhw.spring.finance.service.UserService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Random;

import static ru.vtvhw.spring.finance.enums.TransactionType.EXPENSE;
import static ru.vtvhw.spring.finance.enums.TransactionType.INCOME;

@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationRunner {

    private final UserService userService;
    private final CategoryService categoryService;
    private final TransactionService transactionService;

    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_PASSWORD = "password";
    private static final String TEST_NAME = "Тестовый пользователь";

    private final Random random = new Random();

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("Начало инициализации тестовых данных (dev-профиль)");

        User user;
        try {
            user = userService.getByEmail(TEST_EMAIL);
            log.info("Пользователь с email {} уже существует", TEST_EMAIL);
        } catch (Exception e) {
            user = userService.createUser(TEST_NAME, TEST_EMAIL, TEST_PASSWORD);
            log.info("Создан новый пользователь: {}", TEST_EMAIL);
        }

        var transactions = transactionService.getTransactionsForUser(
                user.getId(),
                LocalDateTime.now().minusYears(1),
                LocalDateTime.now()
        );

        if (!transactions.isEmpty()) {
            log.info("У пользователя уже есть {} транзакций, инициализация пропущена", transactions.size());
            return;
        }

        var incomeCategories = categoryService.getCategoriesByUserAndType(user.getId(), INCOME);
        var expenseCategories = categoryService.getCategoriesByUserAndType(user.getId(), EXPENSE);

        if (incomeCategories.isEmpty() || expenseCategories.isEmpty()) {
            log.warn("Не найдены категории для пользователя. Создаём дефолтные вручную.");
            createDefaultCategories(user);
            incomeCategories = categoryService.getCategoriesByUserAndType(user.getId(), INCOME);
            expenseCategories = categoryService.getCategoriesByUserAndType(user.getId(), EXPENSE);
        }

        int months = 6;
        int transactionsPerMonth = 5;
        var now = LocalDateTime.now();

        for (int monthNb = 0; monthNb < months; monthNb++) {
            var monthStart = now.minusMonths(monthNb).withDayOfMonth(1);
            var daysInMonth = monthStart.toLocalDate().lengthOfMonth();
            var maxDay = (monthNb == 0) ? now.getDayOfMonth() : daysInMonth;

            for (int transactionNb = 0; transactionNb < transactionsPerMonth; transactionNb++) {
                var day = random.nextInt(maxDay) + 1;

                var hour = (monthNb == 0 && day == now.getDayOfMonth())
                        ? random.nextInt(now.getHour() + 1)
                        : random.nextInt(24);
                var minute = (monthNb == 0 && day == now.getDayOfMonth() && hour == now.getHour())
                        ? random.nextInt(now.getMinute() + 1)
                        : random.nextInt(60);
                var second = (monthNb == 0 && day == now.getDayOfMonth() && hour == now.getHour() && minute == now.getMinute())
                        ? random.nextInt(now.getSecond() + 1)
                        : random.nextInt(60);

                var transactionDate = monthStart
                        .withDayOfMonth(day)
                        .withHour(hour)
                        .withMinute(minute)
                        .withSecond(second);

                // Проверка, что дата не в будущем
                if (transactionDate.isAfter(now)) {
                    transactionDate = now;
                }

                var isIncome = random.nextBoolean();
                var amount = BigDecimal.valueOf(random.nextInt(5000) + 100);
                var type = isIncome ? INCOME : EXPENSE;
                var category = isIncome
                        ? incomeCategories.get(random.nextInt(incomeCategories.size()))
                        : expenseCategories.get(random.nextInt(expenseCategories.size()));

                var dto = new TransactionDto();
                dto.setUserId(user.getId());
                dto.setAmount(amount);
                dto.setType(type);
                dto.setCategoryId(category.getId());
                dto.setDate(transactionDate);
                dto.setDescription("Тестовая транзакция #" + (monthNb * transactionsPerMonth + transactionNb + 1));

                transactionService.createTransaction(dto);
            }
        }

        log.info("Создано {} тестовых транзакций для пользователя {}", months * transactionsPerMonth, user.getId());
    }

    private void createDefaultCategories(User user) {
        String[] incomeNames = {"Зарплата", "Фриланс", "Подарки", "Инвестиции"};
        String[] expenseNames = {"Продукты", "Транспорт", "Коммунальные", "Развлечения", "Здоровье", "Одежда", "Образование", "Другое"};

        for (String name : incomeNames) {
            categoryService.createCategory(name, INCOME, user.getId());
        }
        for (String name : expenseNames) {
            categoryService.createCategory(name, EXPENSE, user.getId());
        }
    }
}
