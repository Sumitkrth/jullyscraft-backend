package com.jullyscraft.service.impl;

import com.jullyscraft.dto.request.*;
import com.jullyscraft.dto.response.AuthResponse;
import com.jullyscraft.dto.response.TokenRefreshResponse;
import com.jullyscraft.entity.RefreshToken;
import com.jullyscraft.entity.Role;
import com.jullyscraft.entity.User;
import com.jullyscraft.exception.BadRequestException;
import com.jullyscraft.exception.DuplicateResourceException;
import com.jullyscraft.exception.ResourceNotFoundException;
import com.jullyscraft.exception.UnauthorizedException;
import com.jullyscraft.repository.RefreshTokenRepository;
import com.jullyscraft.repository.RoleRepository;
import com.jullyscraft.repository.UserRepository;
import com.jullyscraft.security.jwt.JwtTokenProvider;
import com.jullyscraft.security.userdetails.UserPrincipal;
import com.jullyscraft.service.AuthService;
import com.jullyscraft.service.NotificationService;
import com.jullyscraft.util.AppConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, Object> redisTemplate;
    private final NotificationService notificationService;

    @Value("${app.jwt.refresh-token-expiry-days:7}")
    private int refreshTokenExpiryDays;

    @Value("${app.jwt.email-verify-expiry-hours:24}")
    private int emailVerifyExpiryHours;

    @Value("${app.jwt.password-reset-expiry-minutes:30}")
    private int passwordResetExpiryMinutes;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException(
                    "Email already registered: " + request.getEmail());
        }

        Role userRole = roleRepository.findByName(Role.RoleName.ROLE_USER)
                .orElseThrow(() -> new ResourceNotFoundException("Default role not found"));

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail().toLowerCase())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .roles(Set.of(userRole))
                .emailVerified(false)
                .enabled(true)
                .build();

        userRepository.save(user);
        sendVerificationEmail(user);
        notificationService.notifyWelcome(user);

        log.info("New user registered: {}", user.getEmail());

        UserPrincipal principal = UserPrincipal.of(user);
        return buildAuthResponse(principal, user);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail().toLowerCase(),
                        request.getPassword()));

        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        userRepository.updateLastLogin(principal.getId(), LocalDateTime.now());

        log.info("User logged in: {}", principal.getEmail());
        return buildAuthResponse(principal,
                userRepository.findById(principal.getId()).orElseThrow());
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        refreshTokenRepository.findByToken(refreshToken).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }

    @Override
    @Transactional
    public TokenRefreshResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken stored = refreshTokenRepository
                .findByToken(request.getRefreshToken())
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (!stored.isValid()) {
            throw new UnauthorizedException("Refresh token expired or revoked");
        }

        UserPrincipal principal = UserPrincipal.of(stored.getUser());
        String newAccessToken = jwtTokenProvider.generateAccessToken(principal);

        // Rotate refresh token
        stored.setRevoked(true);
        refreshTokenRepository.save(stored);
        String newRefreshToken = createRefreshToken(stored.getUser());

        return TokenRefreshResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
    }

    @Override
    @Transactional
    public void verifyEmail(String token) {
        String key = AppConstants.REDIS_OTP_PREFIX + "email-verify:" + token;
        String email = (String) redisTemplate.opsForValue().get(key);

        if (email == null) {
            throw new BadRequestException("Invalid or expired verification link");
        }

        userRepository.markEmailVerified(email);
        redisTemplate.delete(key);
        log.info("Email verified for: {}", email);
    }

    @Override
    public void forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmail(request.getEmail().toLowerCase())
                .ifPresent(user -> {
                    String token = UUID.randomUUID().toString();
                    String key = AppConstants.REDIS_OTP_PREFIX + "pwd-reset:" + token;

                    redisTemplate.opsForValue().set(
                            key, user.getEmail(),
                            passwordResetExpiryMinutes, TimeUnit.MINUTES);
                    notificationService.notifyPasswordReset(user, token);
                });
        // Always return OK to prevent email enumeration
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String key = AppConstants.REDIS_OTP_PREFIX + "pwd-reset:" + request.getToken();
        String email = (String) redisTemplate.opsForValue().get(key);

        if (email == null) {
            throw new BadRequestException("Invalid or expired reset token");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Revoke all refresh tokens for security
        refreshTokenRepository.revokeAllUserTokens(user);
        redisTemplate.delete(key);

        log.info("Password reset for: {}", email);
    }

    @Override
    public void resendVerificationEmail(String email) {
        User user = userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        if (user.isEmailVerified()) {
            throw new BadRequestException("Email is already verified");
        }

        sendVerificationEmail(user);
    }

    // ─── Private Helpers ───────────────────────────────────────────────────────

    private AuthResponse buildAuthResponse(UserPrincipal principal, User user) {
        String accessToken = jwtTokenProvider.generateAccessToken(principal);
        String refreshToken = createRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .emailVerified(user.isEmailVerified())
                .roles(user.getRoles().stream()
                        .map(r -> r.getName().name())
                        .toList())
                .build();
    }

    private String createRefreshToken(User user) {
        // Revoke old tokens first
        refreshTokenRepository.revokeAllUserTokens(user);

        RefreshToken refreshToken = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .expiresAt(LocalDateTime.now().plusDays(refreshTokenExpiryDays))
                .revoked(false)
                .build();

        return refreshTokenRepository.save(refreshToken).getToken();
    }

    private void sendVerificationEmail(User user) {
        String token = UUID.randomUUID().toString();
        String key = AppConstants.REDIS_OTP_PREFIX + "email-verify:" + token;

        redisTemplate.opsForValue().set(
                key, user.getEmail(),
                emailVerifyExpiryHours, TimeUnit.HOURS);
        notificationService.notifyEmailVerification(user, token);
    }
}