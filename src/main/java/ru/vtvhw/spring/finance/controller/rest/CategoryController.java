package ru.vtvhw.spring.finance.controller.rest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ru.vtvhw.spring.finance.dto.category.CategoryResponse;
import ru.vtvhw.spring.finance.dto.category.CreateCategoryRequest;
import ru.vtvhw.spring.finance.dto.category.UpdateCategoryRequest;
import ru.vtvhw.spring.finance.mapper.CategoryMapper;
import ru.vtvhw.spring.finance.service.CategoryService;
import ru.vtvhw.spring.finance.service.UserService;

import java.util.UUID;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Slf4j
public class CategoryController {

    private final CategoryService categoryService;
    private final UserService userService;
    private final CategoryMapper categoryMapper;

    @PostMapping
    public CategoryResponse createCategory(Authentication auth,
                                           @Valid @RequestBody CreateCategoryRequest request) {
        var user = userService.getByEmail(auth.getName());
        var category = categoryService.createCategory(request.getName(), request.getType(), user.getId());
        return categoryMapper.toResponse(category);
    }

    @PutMapping("/{id}")
    public CategoryResponse updateCategory(Authentication auth,
                                           @PathVariable UUID id,
                                           @Valid @RequestBody UpdateCategoryRequest request) {
        var user = userService.getByEmail(auth.getName());
        log.debug("New name: {}", request.getName());
        var category = categoryService.updateCategory(id, request.getName(), user.getId());
        return categoryMapper.toResponse(category);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(Authentication auth, @PathVariable UUID id) {
        var user = userService.getByEmail(auth.getName());
        categoryService.deleteCategory(id, user.getId());
        return ResponseEntity.noContent().build();
    }
}
