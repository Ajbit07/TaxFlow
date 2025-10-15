package com.taxflow.web;

import com.taxflow.application.dto.TaxFlowDtos.AuthResponse;
import com.taxflow.application.dto.TaxFlowDtos.ForgotPasswordRequest;
import com.taxflow.application.dto.TaxFlowDtos.LoginRequest;
import com.taxflow.application.dto.TaxFlowDtos.RefreshRequest;
import com.taxflow.application.dto.TaxFlowDtos.RegisterRequest;
import com.taxflow.application.dto.TaxFlowDtos.ResetPasswordRequest;
import com.taxflow.application.service.AuthService;
import com.taxflow.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.message("Account created", authService.register(request));
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ApiResponse.ok(authService.refresh(request));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request);
        return ApiResponse.message("Logged out", null);
    }

    @PostMapping("/forgot-password")
    public ApiResponse<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        // In a hosted deployment this token is delivered by email. The API returns it
        // directly so the reset flow is fully functional without an SMTP dependency.
        return ApiResponse.message("Reset token issued", Map.of("resetToken", authService.forgotPassword(request)));
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ApiResponse.message("Password reset successful", null);
    }

    @PostMapping("/verify-email")
    public ApiResponse<Void> verifyEmail(@RequestParam("token") String token) {
        authService.verifyEmail(token);
        return ApiResponse.message("Email verified", null);
    }
}
