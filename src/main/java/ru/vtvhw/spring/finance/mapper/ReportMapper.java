package ru.vtvhw.spring.finance.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.vtvhw.spring.finance.dto.report.ReportResponse;
import ru.vtvhw.spring.finance.entity.Report;

@Mapper(componentModel = "spring")
public interface ReportMapper {

    @Mapping(target = "userId", source = "user.id")
    ReportResponse toResponse(Report report);
}
