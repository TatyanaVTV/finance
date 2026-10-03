package ru.vtvhw.spring.finance.controller.mvc;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import ru.vtvhw.spring.finance.dto.user.RegisterUserRequest;
import ru.vtvhw.spring.finance.service.UserService;
import org.springframework.ui.Model;

@Controller
@RequiredArgsConstructor
public class AuthController {
    public static final String USER_ALREADY_EXISTS = "Пользователь с таким email уже существует";

    private final UserService userService;

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        if (!model.containsAttribute("registerForm")) {
            model.addAttribute("registerForm", new RegisterUserRequest());
        }
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registerForm") RegisterUserRequest request,
                           BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "register";
        }

        var email = request.getEmail();
        if (userService.findByEmail(email).isPresent()) {
            bindingResult.rejectValue("email", "duplicate", USER_ALREADY_EXISTS);
            return "register";
        }

        try {
            userService.createUser(request.getName(), request.getEmail(), request.getPassword());
            return "redirect:/login?registered";
        } catch (DataIntegrityViolationException e) {
            bindingResult.rejectValue("email", "duplicate", USER_ALREADY_EXISTS);
            return "register";
        } catch (Exception e) {
            bindingResult.reject("register.error", "Ошибка при регистрации: " + e.getMessage());
            return "register";
        }
    }
}
