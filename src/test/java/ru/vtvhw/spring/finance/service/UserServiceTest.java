package ru.vtvhw.spring.finance.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import ru.vtvhw.spring.finance.entity.User;
import ru.vtvhw.spring.finance.exception.ResourceNotFoundException;
import ru.vtvhw.spring.finance.exception.ValidationException;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public abstract class UserServiceTest {

    protected UserService userService;

    protected abstract void initService();
    protected abstract User createTestUser(String name, String email, String password);

    @BeforeEach
    void setUp() {
        initService();
    }

    @Test
    void createUser_Success() {
        var user = createTestUser("Test User", "test@example.com", "password");
        assertNotNull(user.getId());
        assertEquals("Test User", user.getName());
        assertEquals("test@example.com", user.getEmail());
    }

    @Test
    void createUser_DuplicateEmail_ThrowsException() {
        createTestUser("User1", "duplicate@example.com", "pass");
        assertThrows(RuntimeException.class,
                () -> createTestUser("User2", "duplicate@example.com", "pass"));
    }

    @Test
    void updateUser_Success() {
        var user = createTestUser("Old Name", "old@example.com", "pass");
        var updated = userService.updateUser(user.getId(), "New Name", "new@example.com");
        assertEquals("New Name", updated.getName());
        assertEquals("new@example.com", updated.getEmail());
    }

    @Test
    void updateUser_EmptyName_ThrowsException() {
        var user = createTestUser("Name", "email@example.com", "pass");
        assertThrows(ValidationException.class,
                () -> userService.updateUser(user.getId(), "", "email@example.com"));
    }

    @Test
    void updateUser_EmptyEmail_ThrowsException() {
        var user = createTestUser("Name", "emptyEmail@example.com", "pass");
        assertThrows(ValidationException.class,
                () -> userService.updateUser(user.getId(), "Name", ""));
    }

    @Test
    void updateUser_EmailAlreadyExists_ThrowsException() {
        var user1 = createTestUser("User1", "user1Exists@example.com", "pass");
        var user2 = createTestUser("User2", "user2Exists@example.com", "pass");
        assertThrows(ValidationException.class,
                () -> userService.updateUser(user1.getId(), "User1", "user2Exists@example.com"));
    }

    @Test
    void updateUser_OnlyName_ShouldSucceed() {
        var user = createTestUser("Old Name", "onlyname@example.com", "pass");
        var updated = userService.updateUser(user.getId(), "New Name Only", user.getEmail());
        assertEquals("New Name Only", updated.getName());
        assertEquals(user.getEmail(), updated.getEmail());
    }

    @Test
    void updateUser_OnlyEmail_ShouldSucceed() {
        var user = createTestUser("Old Name", "first@example.com", "pass");
        var updated = userService.updateUser(user.getId(), "Old Name", "second@example.com");
        assertEquals("Old Name", updated.getName());
        assertEquals("second@example.com", updated.getEmail());
    }

    @Test
    void updateUser_UserNotFound_ThrowsException() {
        var randomId = UUID.randomUUID();
        assertThrows(ResourceNotFoundException.class,
                () -> userService.updateUser(randomId, "Name", "email@example.com"));
    }

    @Test
    void getById_Success() {
        var user = createTestUser("Get Me", "get@example.com", "pass");
        var found = userService.getById(user.getId());
        assertEquals(user.getId(), found.getId());
        assertEquals("Get Me", found.getName());
    }

    @Test
    void getById_NotFound_ThrowsException() {
        assertThrows(ResourceNotFoundException.class,
                () -> userService.getById(UUID.randomUUID()));
    }

    @Test
    void findByEmail_Exists_ReturnsUser() {
        var user = createTestUser("Find Me", "find@example.com", "pass");
        var found = userService.findByEmail("find@example.com");
        assertTrue(found.isPresent());
        assertEquals(user.getId(), found.get().getId());
    }

    @Test
    void findByEmail_NotExists_ReturnsEmpty() {
        var found = userService.findByEmail("nonexistent@example.com");
        assertTrue(found.isEmpty());
    }

    @Test
    void getByEmail_Exists_ReturnsUser() {
        var user = createTestUser("Get By Email", "getby@example.com", "pass");
        var found = userService.getByEmail("getby@example.com");
        assertEquals(user.getId(), found.getId());
    }

    @Test
    void getByEmail_NotExists_ThrowsException() {
        assertThrows(ResourceNotFoundException.class,
                () -> userService.getByEmail("nonexistent@example.com"));
    }

    @Test
    void loadUserByUsername_Exists_ReturnsUserDetails() {
        createTestUser("Security User", "security@example.com", "pass");
        var details = userService.loadUserByUsername("security@example.com");
        assertNotNull(details);
        assertEquals("security@example.com", details.getUsername());
    }

    @Test
    void loadUserByUsername_NotExists_ThrowsUsernameNotFoundException() {
        assertThrows(UsernameNotFoundException.class,
                () -> userService.loadUserByUsername("unknown@example.com"));
    }
}
