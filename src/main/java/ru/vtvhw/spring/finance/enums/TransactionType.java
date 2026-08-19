package ru.vtvhw.spring.finance.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum TransactionType {
    INCOME("Доход"),
    EXPENSE("Расход");

    private final String value;
}
