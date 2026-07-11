package ru.vtvhw.spring.finance.controller.rest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.vtvhw.spring.finance.dto.CategoryDto;
import ru.vtvhw.spring.finance.entity.Category;
import ru.vtvhw.spring.finance.entity.User;
import ru.vtvhw.spring.finance.mapper.CategoryMapper;
import ru.vtvhw.spring.finance.service.CategoryService;
import ru.vtvhw.spring.finance.service.UserService;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.vtvhw.spring.finance.enums.TransactionType.INCOME;
import static ru.vtvhw.spring.finance.exception.ResourceNotFoundException.categoryNotFound;
import static ru.vtvhw.spring.finance.exception.ValidationException.*;

@WebMvcTest(CategoryController.class)
@WithMockUser(username = "test@example.com")
public class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryService categoryService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private CategoryMapper categoryMapper;

    private final UUID userId = UUID.randomUUID();
    private final UUID categoryId = UUID.randomUUID();
    private final User user = User.builder().id(userId).name("Test User").email("test@example.com").build();
    private final Category category = Category.builder().id(categoryId).name("Salary").type(INCOME).user(user).build();
    private final CategoryDto categoryDto = CategoryDto.builder().id(categoryId).name("Salary").type(INCOME).userId(userId).build();

    @Test
    void createCategory_Success() throws Exception {
        when(userService.getByEmail("test@example.com")).thenReturn(user);
        when(categoryService.createCategory("Salary", INCOME, userId)).thenReturn(category);
        when(categoryMapper.toDto(category)).thenReturn(categoryDto);

        mockMvc.perform(post("/api/categories")
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\":\"Salary\",\"type\":\"INCOME\"}")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(categoryId.toString()))
                .andExpect(jsonPath("$.name").value("Salary"))
                .andExpect(jsonPath("$.type").value("INCOME"))
                .andExpect(jsonPath("$.userId").value(userId.toString()));
    }

    @Test
    void createCategory_DuplicateName_ThrowsValidationException() throws Exception {
        when(userService.getByEmail("test@example.com")).thenReturn(user);
        doThrow(categoryAlreadyExists("Salary"))
                .when(categoryService).createCategory("Salary", INCOME, userId);

        mockMvc.perform(post("/api/categories")
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\":\"Salary\",\"type\":\"INCOME\"}")
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Категория с именем 'Salary' уже существует"));
    }

    @Test
    void updateCategory_Success() throws Exception {
        when(userService.getByEmail("test@example.com")).thenReturn(user);
        when(categoryService.updateCategory(eq(categoryId), eq("New Name"), eq(userId))).thenReturn(category);
        when(categoryMapper.toDto(category)).thenReturn(categoryDto);

        mockMvc.perform(put("/api/categories/{id}", categoryId)
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\":\"New Name\"}")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(categoryId.toString()))
                .andExpect(jsonPath("$.name").value("Salary"));
    }

    @Test
    void updateCategory_EmptyName_ThrowsValidationException() throws Exception {
        when(userService.getByEmail("test@example.com")).thenReturn(user);
        doThrow(emptyCategoryName())
                .when(categoryService).updateCategory(eq(categoryId), eq(""), eq(userId));

        mockMvc.perform(put("/api/categories/{id}", categoryId)
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\":\"\"}")
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Название категории не может быть пустым"));
    }

    @Test
    void updateCategory_CategoryNotFound_ThrowsResourceNotFoundException() throws Exception {
        when(userService.getByEmail("test@example.com")).thenReturn(user);
        doThrow(categoryNotFound(categoryId))
                .when(categoryService).updateCategory(eq(categoryId), eq("New Name"), eq(userId));

        mockMvc.perform(put("/api/categories/{id}", categoryId)
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\":\"New Name\"}")
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Категория не найдена: " + categoryId));
    }

    @Test
    void deleteCategory_Success() throws Exception {
        when(userService.getByEmail("test@example.com")).thenReturn(user);

        mockMvc.perform(delete("/api/categories/{id}", categoryId)
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteCategory_HasTransactions_ThrowsValidationException() throws Exception {
        when(userService.getByEmail("test@example.com")).thenReturn(user);
        doThrow(categoryHasTransactions(categoryId))
                .when(categoryService).deleteCategory(categoryId, userId);

        mockMvc.perform(delete("/api/categories/{id}", categoryId)
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Невозможно удалить категорию (ID: " + categoryId + "), так как существуют транзакции, ссылающиеся на неё. Сначала переназначьте или удалите эти транзакции."));
    }

    @Test
    void deleteCategory_CategoryNotFound_ThrowsResourceNotFoundException() throws Exception {
        when(userService.getByEmail("test@example.com")).thenReturn(user);
        doThrow(categoryNotFound(categoryId))
                .when(categoryService).deleteCategory(categoryId, userId);

        mockMvc.perform(delete("/api/categories/{id}", categoryId)
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Категория не найдена: " + categoryId));
    }
}
