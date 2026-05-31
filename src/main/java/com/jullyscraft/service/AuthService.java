package com.jullyscraft.service;

import com.jullyscraft.dto.request.*;
import com.jullyscraft.dto.response.AuthResponse;
import com.jullyscraft.dto.response.TokenRefreshResponse;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    void logout(String refreshToken);
    TokenRefreshResponse refreshToken(RefreshTokenRequest request);
    void verifyEmail(String token);
    void forgotPassword(ForgotPasswordRequest request);
    void resetPassword(ResetPasswordRequest request);
    void resendVerificationEmail(String email);
}