package ru.vtvhw.spring.finance.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.vtvhw.spring.finance.dto.CategoryDto;
import ru.vtvhw.spring.finance.entity.Category;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    @Mapping(target = "userId", source = "user.id")
    CategoryDto toDto(Category category);

    @Mapping(target = "user", ignore = true)
    Category toEntity(CategoryDto dto);
}
