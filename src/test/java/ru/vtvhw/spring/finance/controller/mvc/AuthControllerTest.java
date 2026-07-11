package ru.vtvhw.spring.finance.controller.mvc;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.vtvhw.spring.finance.entity.User;
import ru.vtvhw.spring.finance.service.UserService;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    void showRegistrationForm_ShouldReturnRegisterView() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));
    }

    @Test
    void register_NewUser_ShouldRedirectToLogin() throws Exception {
        when(userService.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userService.createUser(anyString(), anyString(), anyString())).thenReturn(new User());

        mockMvc.perform(post("/register")
                        .param("name", "John")
                        .param("email", "john@example.com")
                        .param("password", "pass"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered"));
    }

    @Test
    void register_UserAlreadyExists_ShouldShowError() throws Exception {
        when(userService.findByEmail("existing@example.com"))
                .thenReturn(Optional.of(new User()));

        mockMvc.perform(post("/register")
                        .param("name", "Jane")
                        .param("email", "existing@example.com")
                        .param("password", "pass"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeExists("error"))
                .andExpect(model().attribute("error", "Пользователь с таким email уже существует"))
                .andExpect(model().attribute("name", "Jane"))
                .andExpect(model().attribute("email", "existing@example.com"));
    }

    @Test
    void register_DataIntegrityViolation_ShouldShowError() throws Exception {
        when(userService.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userService.createUser(anyString(), anyString(), anyString()))
                .thenThrow(new DataIntegrityViolationException("Duplicate email"));

        mockMvc.perform(post("/register")
                        .param("name", "John")
                        .param("email", "duplicate@example.com")
                        .param("password", "pass"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeExists("error"))
                .andExpect(model().attribute("error", "Пользователь с таким email уже существует"))
                .andExpect(model().attribute("name", "John"))
                .andExpect(model().attribute("email", "duplicate@example.com"));
    }

    @Test
    void register_GenericException_ShouldShowError() throws Exception {
        when(userService.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userService.createUser(anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("Database down"));

        mockMvc.perform(post("/register")
                        .param("name", "John")
                        .param("email", "john@example.com")
                        .param("password", "pass"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeExists("error"))
                .andExpect(model().attribute("error", "Ошибка при регистрации: Database down"))
                .andExpect(model().attribute("name", "John"))
                .andExpect(model().attribute("email", "john@example.com"));
    }
}
