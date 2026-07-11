package ru.vtvhw.spring.finance.controller.rest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ru.vtvhw.spring.finance.dto.CategoryDto;
import ru.vtvhw.spring.finance.mapper.CategoryMapper;
import ru.vtvhw.spring.finance.service.CategoryService;
import ru.vtvhw.spring.finance.service.UserService;

import java.util.Map;
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
    public CategoryDto createCategory(Authentication auth, @RequestBody CategoryDto dto) {
        var user = userService.getByEmail(auth.getName());
        var category = categoryService.createCategory(dto.getName(), dto.getType(), user.getId());
        return categoryMapper.toDto(category);
    }

    @PutMapping("/{id}")
    public CategoryDto updateCategory(Authentication auth,
                                      @PathVariable UUID id,
                                      @RequestBody Map<String, String> payload) {
        var user = userService.getByEmail(auth.getName());
        var newName = payload.get("name");
        log.debug("payload name: {}", newName);
        var category = categoryService.updateCategory(id, newName, user.getId());
        return categoryMapper.toDto(category);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(Authentication auth, @PathVariable UUID id) {
        var user = userService.getByEmail(auth.getName());
        categoryService.deleteCategory(id, user.getId());
        return ResponseEntity.noContent().build();
    }
}
