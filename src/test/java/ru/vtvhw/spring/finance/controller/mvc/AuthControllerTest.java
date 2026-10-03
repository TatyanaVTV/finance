package ru.vtvhw.spring.finance.controller.mvc;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import ru.vtvhw.spring.finance.entity.User;
import ru.vtvhw.spring.finance.service.UserService;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static ru.vtvhw.spring.finance.controller.mvc.AuthController.USER_ALREADY_EXISTS;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
public class AuthControllerTest {
    private static final String REGISTER_FORM_ATTR = "registerForm";

    private static final String TEST_USER_1 = "John";
    private static final String TEST_USER_2 = "Jane";

    private static final String TEST_PSWD = "password";

    private static final String TEST_EMAIL = "john@example.com";
    private static final String EXISTING_EMAIL = "existing@example.com";
    private static final String DUPLICATE_EMAIL = "duplicate@example.com";
    private static final String INVALID_EMAIL = "not-an-email";

    // 64 (local) + '@' + 51 (label1) + '.' + 30 (label2) + '.com' = 151
    // Все метки <= 63, локальная часть = 64 (граница RFC), общая длина > 150
    private static final String TOO_LONG_EMAIL =
            "a".repeat(64) + "@" + "b".repeat(51) + "." + "c".repeat(30) + ".com"; // 151
    private static final String TOO_LONG_PSWD = "a".repeat(65);

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
                        .param("name", TEST_USER_1)
                        .param("email", TEST_EMAIL)
                        .param("password", TEST_PSWD))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered"));
    }

    @Test
    void register_UserAlreadyExists_ShouldShowError() throws Exception {
        when(userService.findByEmail(EXISTING_EMAIL))
                .thenReturn(Optional.of(new User()));

        mockMvc.perform(post("/register")
                        .param("name", TEST_USER_2)
                        .param("email", EXISTING_EMAIL)
                        .param("password", TEST_PSWD))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeHasFieldErrors(REGISTER_FORM_ATTR, "email"))
                .andExpect(model().attributeHasFieldErrorCode(REGISTER_FORM_ATTR, "email", "duplicate"))
                .andExpect(hasFieldError("email", USER_ALREADY_EXISTS))
                .andExpect(model().attribute(REGISTER_FORM_ATTR,
                        hasProperty("email", is(EXISTING_EMAIL))));
    }

    @Test
    void register_DataIntegrityViolation_ShouldShowError() throws Exception {
        when(userService.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userService.createUser(anyString(), anyString(), anyString()))
                .thenThrow(new DataIntegrityViolationException("Duplicate email"));

        mockMvc.perform(post("/register")
                        .param("name", TEST_USER_1)
                        .param("email", DUPLICATE_EMAIL)
                        .param("password", TEST_PSWD))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeHasFieldErrors(REGISTER_FORM_ATTR, "email"))
                .andExpect(model().attributeHasFieldErrorCode(REGISTER_FORM_ATTR, "email", "duplicate"))
                .andExpect(hasFieldError("email", USER_ALREADY_EXISTS))
                .andExpect(model().attribute(REGISTER_FORM_ATTR,
                        hasProperty("name", is(TEST_USER_1))))
                .andExpect(model().attribute(REGISTER_FORM_ATTR,
                        hasProperty("email", is(DUPLICATE_EMAIL))));
    }

    @Test
    void register_GenericException_ShouldShowError() throws Exception {
        when(userService.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userService.createUser(anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("Database down"));

        mockMvc.perform(post("/register")
                        .param("name", TEST_USER_1)
                        .param("email", TEST_EMAIL)
                        .param("password", TEST_PSWD))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeHasErrors(REGISTER_FORM_ATTR))
                .andExpect(hasGlobalError("Ошибка при регистрации: Database down"));
    }

    @Test
    void register_EmptyName_ReturnsRegisterViewWithErrors() throws Exception {
        mockMvc.perform(post("/register")
                        .param("name", "")
                        .param("email", TEST_EMAIL)
                        .param("password", TEST_PSWD))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeHasFieldErrors(REGISTER_FORM_ATTR, "name"))
                .andExpect(hasFieldError("name", "Имя обязательно"));

        verify(userService, never()).createUser(any(), any(), any());
    }

    @Test
    void register_InvalidEmail_ReturnsRegisterViewWithErrors() throws Exception {
        mockMvc.perform(post("/register")
                        .param("name", TEST_USER_1)
                        .param("email", INVALID_EMAIL)
                        .param("password", TEST_PSWD))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeHasFieldErrors(REGISTER_FORM_ATTR, "email"))
                .andExpect(hasFieldError("email", "Некорректный email"));

        verify(userService, never()).createUser(any(), any(), any());
    }

    @Test
    void register_TooLongEmail_ReturnsRegisterViewWithErrors() throws Exception {
        mockMvc.perform(post("/register")
                        .param("name", TEST_USER_1)
                        .param("email", TOO_LONG_EMAIL)
                        .param("password", TEST_PSWD))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeHasFieldErrors(REGISTER_FORM_ATTR, "email"))
                .andExpect(hasFieldError("email", "Email не должен превышать 150 символов"));

        verify(userService, never()).createUser(any(), any(), any());
    }

    @Test
    void register_TooLongPswd_ReturnsRegisterViewWithErrors() throws Exception {
        mockMvc.perform(post("/register")
                        .param("name", TEST_USER_1)
                        .param("email", TEST_EMAIL)
                        .param("password", TOO_LONG_PSWD))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeHasFieldErrors(REGISTER_FORM_ATTR, "password"))
                .andExpect(hasFieldError("password", "Пароль должен содержать от 6 до 64 символов"));

        verify(userService, never()).createUser(any(), any(), any());
    }

    private static ResultMatcher hasFieldError(String field, String expectedMessage) {
        return result -> {
            var bindingResult = (BindingResult) result.getRequest()
                    .getAttribute(BindingResult.MODEL_KEY_PREFIX + REGISTER_FORM_ATTR);
            assertThat(bindingResult)
                    .as("BindingResult must be in request attributes under '%s'",
                            BindingResult.MODEL_KEY_PREFIX + REGISTER_FORM_ATTR)
                    .isNotNull();
            assertThat(bindingResult.getFieldErrors(field))
                    .as("Field errors for '%s' in '%s'", field, REGISTER_FORM_ATTR)
                    .extracting(FieldError::getDefaultMessage)
                    .contains(expectedMessage);
        };
    }

    private static ResultMatcher hasGlobalError(String expectedMessage) {
        return result -> {
            var bindingResult = (BindingResult) result.getRequest()
                    .getAttribute(BindingResult.MODEL_KEY_PREFIX + REGISTER_FORM_ATTR);
            assertThat(bindingResult)
                    .as("BindingResult must be in request attributes under '%s'",
                            BindingResult.MODEL_KEY_PREFIX + REGISTER_FORM_ATTR)
                    .isNotNull();
            assertThat(bindingResult.getGlobalErrors())
                    .as("Global errors in '%s'", REGISTER_FORM_ATTR)
                    .extracting(ObjectError::getDefaultMessage)
                    .contains(expectedMessage);
        };
    }
}
