package ru.vtvhw.spring.finance.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.vtvhw.spring.finance.dto.category.CategoryResponse;
import ru.vtvhw.spring.finance.entity.Category;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    @Mapping(target = "userId", source = "user.id")
    CategoryResponse toResponse(Category category);
}
