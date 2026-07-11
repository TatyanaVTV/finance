package ru.vtvhw.spring.finance.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.vtvhw.spring.finance.dto.TransactionDto;
import ru.vtvhw.spring.finance.entity.Transaction;
import ru.vtvhw.spring.finance.entity.User;
import ru.vtvhw.spring.finance.enums.TransactionType;
import ru.vtvhw.spring.finance.exception.ValidationException;
import ru.vtvhw.spring.finance.mapper.TransactionMapper;
import ru.vtvhw.spring.finance.repository.CategoryRepository;
import ru.vtvhw.spring.finance.repository.TransactionRepository;
import ru.vtvhw.spring.finance.repository.UserRepository;
import ru.vtvhw.spring.finance.service.impl.TransactionServiceImpl;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static ru.vtvhw.spring.finance.enums.TransactionType.INCOME;

@ExtendWith(MockitoExtension.class)
public class TransactionServiceTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private UserRepository userRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private TransactionMapper transactionMapper;
    @InjectMocks private TransactionServiceImpl service;

    @Test
    public void createTransaction_ValidData_ShouldSucceed() {
        var userId = UUID.randomUUID();
        var user = User.builder().id(userId).build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        var dto = new TransactionDto();
        dto.setUserId(userId);
        dto.setAmount(BigDecimal.valueOf(100));
        dto.setType(INCOME);

        var entity = Transaction.builder().amount(BigDecimal.valueOf(100)).build();

        when(transactionMapper.toEntity(any())).thenReturn(entity);
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.createTransaction(dto);
        assertNotNull(result);
        assertEquals(BigDecimal.valueOf(100), result.getAmount());
    }

    @Test
    public void createTransaction_NegativeAmount_ShouldThrow() {
        var dto = new TransactionDto();
        dto.setAmount(BigDecimal.valueOf(-10));
        assertThrows(ValidationException.class, () -> service.createTransaction(dto));
    }
}
