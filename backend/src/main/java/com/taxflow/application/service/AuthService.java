package com.taxflow.application.service;

import com.taxflow.application.dto.TaxFlowDtos.AuthResponse;
import com.taxflow.application.dto.TaxFlowDtos.ChangePasswordRequest;
import com.taxflow.application.dto.TaxFlowDtos.ForgotPasswordRequest;
import com.taxflow.application.dto.TaxFlowDtos.LoginRequest;
import com.taxflow.application.dto.TaxFlowDtos.RefreshRequest;
import com.taxflow.application.dto.TaxFlowDtos.RegisterRequest;
import com.taxflow.application.dto.TaxFlowDtos.ResetPasswordRequest;
import com.taxflow.common.InputSanitizer;
import com.taxflow.common.exception.BusinessException;
import com.taxflow.common.exception.NotFoundException;
import com.taxflow.domain.enums.UserRole;
import com.taxflow.domain.model.RefreshToken;
import com.taxflow.domain.model.User;
import com.taxflow.infrastructure.repository.RefreshTokenRepository;
import com.taxflow.infrastructure.repository.UserRepository;
import com.taxflow.security.JwtProperties;
import com.taxflow.security.JwtService;
import com.taxflow.security.SecurityUtils;
import com.taxflow.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final InputSanitizer sanitizer;
    private final AuditService auditService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = sanitizer.clean(request.email()).toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessException("Email already registered");
        }
        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(request.password()))
                .fullName(sanitizer.clean(request.fullName()))
                .phone(sanitizer.clean(request.phone()))
                .role(request.role() == null ? UserRole.BUSINESS_OWNER : request.role())
                .enabled(true)
                .emailVerified(false)
                .verificationToken(UUID.randomUUID().toString())
                .build();
        userRepository.save(user);
        auditService.log(null, "REGISTER", "USER", user.getId(), null, email);
        return issueTokens(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        User user = userRepository.findByEmailIgnoreCase(request.email()).orElseThrow(() -> new NotFoundException("User not found"));
        user.setLastLoginAt(OffsetDateTime.now());
        auditService.log(null, "LOGIN", "USER", user.getId(), null, user.getEmail());
        return issueTokens(user);
    }

    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        RefreshToken token = refreshTokenRepository.findByTokenHashAndRevokedFalse(hash(request.refreshToken()))
                .orElseThrow(() -> new BusinessException("Invalid refresh token"));
        if (token.getExpiresAt().isBefore(OffsetDateTime.now())) {
            token.setRevoked(true);
            throw new BusinessException("Refresh token expired");
        }
        token.setRevoked(true);
        return issueTokens(token.getUser());
    }

    @Transactional
    public void logout(RefreshRequest request) {
        refreshTokenRepository.findByTokenHashAndRevokedFalse(hash(request.refreshToken()))
                .ifPresent(token -> token.setRevoked(true));
        auditService.log(null, "LOGOUT", "USER", null, null, "session revoked");
    }

    @Transactional
    public String forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.email()).orElseThrow(() -> new NotFoundException("User not found"));
        user.setResetToken(UUID.randomUUID().toString());
        user.setResetTokenExpiresAt(OffsetDateTime.now().plusMinutes(30));
        return user.getResetToken();
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByResetToken(request.token()).orElseThrow(() -> new BusinessException("Invalid reset token"));
        if (user.getResetTokenExpiresAt() == null || user.getResetTokenExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new BusinessException("Reset token expired");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setResetToken(null);
        user.setResetTokenExpiresAt(null);
        auditService.log(null, "RESET_PASSWORD", "USER", user.getId(), null, "password changed");
    }

    @Transactional
    public void verifyEmail(String token) {
        User user = userRepository.findByVerificationToken(token).orElseThrow(() -> new BusinessException("Invalid verification token"));
        user.setEmailVerified(true);
        user.setVerificationToken(null);
        auditService.log(null, "VERIFY_EMAIL", "USER", user.getId(), null, "email verified");
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        User user = userRepository.findById(SecurityUtils.userId()).orElseThrow(() -> new NotFoundException("User not found"));
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BusinessException("Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        auditService.log(null, "CHANGE_PASSWORD", "USER", user.getId(), null, "password changed");
    }

    private AuthResponse issueTokens(User user) {
        UserPrincipal principal = UserPrincipal.from(user);
        String refresh = UUID.randomUUID() + "." + UUID.randomUUID();
        refreshTokenRepository.save(RefreshToken.builder()
                .user(user)
                .tokenHash(hash(refresh))
                .expiresAt(OffsetDateTime.now().plusDays(jwtProperties.refreshTokenDays()))
                .revoked(false)
                .build());
        return new AuthResponse(user.getId(), user.getEmail(), user.getFullName(), user.getRole(),
                jwtService.accessToken(principal), refresh, user.isEmailVerified());
    }

    private String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to hash token", ex);
        }
    }
}
