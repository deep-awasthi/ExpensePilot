package com.flowbridge.finance.application.port.out;

import com.flowbridge.finance.application.port.in.TokenPair;
import com.flowbridge.finance.domain.model.User;

public interface TokenProvider {
    TokenPair generateTokenPair(User user);
    TokenPair refreshTokens(String refreshToken);
    void revokeToken(String token);
    boolean isTokenRevoked(String token);
}
