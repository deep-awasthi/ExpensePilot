package com.flowbridge.finance.application.service;

import com.flowbridge.finance.application.port.in.LoginCommand;
import com.flowbridge.finance.application.port.in.RegisterCommand;
import com.flowbridge.finance.application.port.in.TokenPair;
import com.flowbridge.finance.application.port.out.AuditEventRepository;
import com.flowbridge.finance.application.port.out.PasswordHasher;
import com.flowbridge.finance.application.port.out.TokenProvider;
import com.flowbridge.finance.application.port.out.UserRepository;
import com.flowbridge.finance.domain.exception.AuthenticationException;
import com.flowbridge.finance.domain.exception.EmailAlreadyExistsException;
import com.flowbridge.finance.domain.model.AuditEvent;
import com.flowbridge.finance.domain.model.Role;
import com.flowbridge.finance.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    private UserRepository userRepository;
    private PasswordHasher passwordHasher;
    private TokenProvider tokenProvider;
    private AuditEventRepository auditEventRepository;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordHasher = mock(PasswordHasher.class);
        tokenProvider = mock(TokenProvider.class);
        auditEventRepository = mock(AuditEventRepository.class);
        authService = new AuthService(userRepository, passwordHasher, tokenProvider, auditEventRepository);
    }

    @Test
    void register_shouldCreateAdminUser_whenFirstUser() {
        // Given
        RegisterCommand command = new RegisterCommand("Admin User", "admin@test.com", "securepassword");
        when(userRepository.existsByEmail(command.getEmail())).thenReturn(false);
        when(userRepository.count()).thenReturn(0L);
        when(passwordHasher.hash(command.getPassword())).thenReturn("hashedpassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(tokenProvider.generateTokenPair(any(User.class))).thenReturn(new TokenPair("access", "refresh"));

        // When
        TokenPair tokens = authService.register(command);

        // Then
        assertNotNull(tokens);
        assertEquals("access", tokens.getAccessToken());
        
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertEquals(Role.ADMIN, savedUser.getRole());
        assertEquals("admin@test.com", savedUser.getEmail());
        assertEquals("hashedpassword", savedUser.getPassword());

        verify(auditEventRepository).save(any(AuditEvent.class));
    }

    @Test
    void register_shouldCreateNormalUser_whenNotFirstUser() {
        // Given
        RegisterCommand command = new RegisterCommand("Normal User", "user@test.com", "securepassword");
        when(userRepository.existsByEmail(command.getEmail())).thenReturn(false);
        when(userRepository.count()).thenReturn(1L);
        when(passwordHasher.hash(command.getPassword())).thenReturn("hashedpassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(tokenProvider.generateTokenPair(any(User.class))).thenReturn(new TokenPair("access", "refresh"));

        // When
        TokenPair tokens = authService.register(command);

        // Then
        assertNotNull(tokens);
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertEquals(Role.USER, savedUser.getRole());
    }

    @Test
    void register_shouldThrowException_whenEmailAlreadyExists() {
        // Given
        RegisterCommand command = new RegisterCommand("Duplicate User", "duplicate@test.com", "password");
        when(userRepository.existsByEmail(command.getEmail())).thenReturn(true);

        // When/Then
        assertThrows(EmailAlreadyExistsException.class, () -> authService.register(command));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void login_shouldReturnTokens_whenCredentialsAreValid() {
        // Given
        LoginCommand command = new LoginCommand("user@test.com", "password");
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("user@test.com")
                .password("hashedpassword")
                .role(Role.USER)
                .build();

        when(userRepository.findByEmail(command.getEmail())).thenReturn(Optional.of(user));
        when(passwordHasher.matches(command.getPassword(), user.getPassword())).thenReturn(true);
        when(tokenProvider.generateTokenPair(user)).thenReturn(new TokenPair("access", "refresh"));

        // When
        TokenPair tokens = authService.login(command);

        // Then
        assertNotNull(tokens);
        assertEquals("access", tokens.getAccessToken());
        verify(auditEventRepository).save(any(AuditEvent.class));
    }

    @Test
    void login_shouldThrowException_whenUserNotFound() {
        // Given
        LoginCommand command = new LoginCommand("unknown@test.com", "password");
        when(userRepository.findByEmail(command.getEmail())).thenReturn(Optional.empty());

        // When/Then
        assertThrows(AuthenticationException.class, () -> authService.login(command));
    }

    @Test
    void login_shouldThrowException_whenPasswordIncorrect() {
        // Given
        LoginCommand command = new LoginCommand("user@test.com", "wrongpassword");
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("user@test.com")
                .password("hashedpassword")
                .role(Role.USER)
                .build();

        when(userRepository.findByEmail(command.getEmail())).thenReturn(Optional.of(user));
        when(passwordHasher.matches(command.getPassword(), user.getPassword())).thenReturn(false);

        // When/Then
        assertThrows(AuthenticationException.class, () -> authService.login(command));
    }
}
