package ru.vtvhw.spring.finance.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.vtvhw.spring.finance.service.UserService;
import org.springframework.ui.Model;

@Controller
@RequiredArgsConstructor
public class AuthController {
    private static final String USER_ALREADY_EXISTS = "Пользователь с таким email уже существует";

    private final UserService userService;

    @GetMapping("/register")
    public String showRegistrationForm() {
        return "register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String name,
                           @RequestParam String email,
                           @RequestParam String password,
                           Model model) {
        if (userService.findByEmail(email).isPresent()) {
            prepareErrorModel(model, USER_ALREADY_EXISTS, name, email);
            return "register";
        }

        try {
            userService.createUser(name, email, password);
            return "redirect:/login?registered";
        } catch (DataIntegrityViolationException e) {
            prepareErrorModel(model, USER_ALREADY_EXISTS, name, email);
            return "register";
        } catch (Exception e) {
            prepareErrorModel(model, "Ошибка при регистрации: " + e.getMessage(), name, email);
            return "register";
        }
    }

    private void prepareErrorModel(Model model, String errorMessage, String name, String email) {
        model.addAttribute("error", errorMessage);
        model.addAttribute("name", name);
        model.addAttribute("email", email);
    }
}
