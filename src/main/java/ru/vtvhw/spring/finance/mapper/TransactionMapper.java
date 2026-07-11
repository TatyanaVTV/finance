package ru.vtvhw.spring.finance.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.vtvhw.spring.finance.dto.TransactionDto;
import ru.vtvhw.spring.finance.entity.Transaction;

@Mapper(componentModel = "spring")
public interface TransactionMapper {

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "type", source = "type")
    TransactionDto toDto(Transaction transaction);

    @Mapping(target = "user", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Transaction toEntity(TransactionDto dto);
}
