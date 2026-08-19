package ru.vtvhw.spring.finance.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class UserDto {
    private UUID id;
    private String name;
    private String email;
}
