package ru.vtvhw.spring.finance.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.vtvhw.spring.finance.entity.User;
import ru.vtvhw.spring.finance.service.UserService;
import ru.vtvhw.spring.finance.service.UserServiceTest;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class UserServiceImplTest extends UserServiceTest {

    @Autowired
    private UserService userServiceImpl;

    @Override
    protected void initService() {
        this.userService = userServiceImpl;
    }

    @Override
    protected User createTestUser(String name, String email, String password) {
        return userService.createUser(name, email, password);
    }
}
