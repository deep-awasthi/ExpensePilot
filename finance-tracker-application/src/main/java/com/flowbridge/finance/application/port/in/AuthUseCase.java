package com.flowbridge.finance.application.port.in;

public interface AuthUseCase {
    TokenPair register(RegisterCommand command);
    TokenPair login(LoginCommand command);
    TokenPair refresh(String refreshToken);
    void logout(String accessToken);
}
