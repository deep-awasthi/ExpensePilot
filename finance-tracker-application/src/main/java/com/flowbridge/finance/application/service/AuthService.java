package com.flowbridge.finance.application.service;

import com.flowbridge.finance.application.port.in.AuthUseCase;
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
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class AuthService implements AuthUseCase {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final TokenProvider tokenProvider;
    private final AuditEventRepository auditEventRepository;

    public AuthService(UserRepository userRepository,
                       PasswordHasher passwordHasher,
                       TokenProvider tokenProvider,
                       AuditEventRepository auditEventRepository) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.tokenProvider = tokenProvider;
        this.auditEventRepository = auditEventRepository;
    }

    @Override
    public TokenPair register(RegisterCommand command) {
        if (userRepository.existsByEmail(command.getEmail())) {
            throw new EmailAlreadyExistsException("Email already exists: " + command.getEmail());
        }

        // The first user in the system becomes ADMIN, subsequent users are USER
        Role role = userRepository.count() == 0 ? Role.ADMIN : Role.USER;

        User user = User.builder()
                .id(UUID.randomUUID())
                .name(command.getName())
                .email(command.getEmail())
                .password(passwordHasher.hash(command.getPassword()))
                .role(role)
                .createdAt(Instant.now())
                .build();

        User savedUser = userRepository.save(user);

        // Audit the registration
        auditEventRepository.save(AuditEvent.builder()
                .id(UUID.randomUUID())
                .eventType("USER_REGISTERED")
                .aggregateId(savedUser.getId().toString())
                .payload("User registered with email: " + savedUser.getEmail() + ", role: " + savedUser.getRole())
                .timestamp(Instant.now())
                .build());

        return tokenProvider.generateTokenPair(savedUser);
    }

    @Override
    public TokenPair login(LoginCommand command) {
        User user = userRepository.findByEmail(command.getEmail())
                .orElseThrow(() -> new AuthenticationException("Invalid email or password"));

        if (!passwordHasher.matches(command.getPassword(), user.getPassword())) {
            throw new AuthenticationException("Invalid email or password");
        }

        // Audit the successful login
        auditEventRepository.save(AuditEvent.builder()
                .id(UUID.randomUUID())
                .eventType("USER_LOGIN")
                .aggregateId(user.getId().toString())
                .payload("User logged in successfully: " + user.getEmail())
                .timestamp(Instant.now())
                .build());

        return tokenProvider.generateTokenPair(user);
    }

    @Override
    public TokenPair refresh(String refreshToken) {
        return tokenProvider.refreshTokens(refreshToken);
    }

    @Override
    public void logout(String accessToken) {
        tokenProvider.revokeToken(accessToken);
    }
}
