package com.jullyscraft.controller;

import com.jullyscraft.dto.request.*;
import com.jullyscraft.dto.response.ApiResponse;
import com.jullyscraft.dto.response.AuthResponse;
import com.jullyscraft.dto.response.TokenRefreshResponse;
import com.jullyscraft.service.AuthService;
import com.jullyscraft.util.AppConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(AppConstants.AUTH_BASE)
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register, login, token refresh, password management")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Registration successful", response));
    }

    @PostMapping("/login")
    @Operation(summary = "Login and receive JWT tokens")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout and revoke refresh token")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestParam String refreshToken) {
        authService.logout(refreshToken);
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully"));
    }

    @PostMapping("/refresh-token")
    @Operation(summary = "Get new access token using refresh token")
    public ResponseEntity<ApiResponse<TokenRefreshResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest request) {
        TokenRefreshResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success("Token refreshed", response));
    }

    @GetMapping("/verify-email")
    @Operation(summary = "Verify email address via token")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(
            @RequestParam String token) {
        authService.verifyEmail(token);
        return ResponseEntity.ok(ApiResponse.success("Email verified successfully"));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Send password reset link to email")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(
                ApiResponse.success("If this email exists, a reset link has been sent"));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password using token from email")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password reset successfully"));
    }

    @PostMapping("/resend-verification")
    @Operation(summary = "Resend email verification link")
    public ResponseEntity<ApiResponse<Void>> resendVerification(
            @RequestParam String email) {
        authService.resendVerificationEmail(email);
        return ResponseEntity.ok(ApiResponse.success("Verification email sent"));
    }
}