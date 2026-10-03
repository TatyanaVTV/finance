package ru.vtvhw.spring.finance.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import ru.vtvhw.spring.finance.dto.report.ReportExportRequest;

import static java.util.Objects.isNull;

public class DateRangeValidator implements ConstraintValidator<ValidDateRange, ReportExportRequest> {

    @Override
    public boolean isValid(ReportExportRequest request, ConstraintValidatorContext context) {
        if (isNull(request) || isNull(request.getFrom()) || isNull(request.getTo())) {
            return true;
        }
        return !request.getFrom().isAfter(request.getTo());
    }
}
