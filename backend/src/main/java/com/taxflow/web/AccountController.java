package com.taxflow.web;

import com.taxflow.application.dto.TaxFlowDtos.AdminUserResponse;
import com.taxflow.application.dto.TaxFlowDtos.ChangePasswordRequest;
import com.taxflow.application.service.AccessService;
import com.taxflow.application.service.AuthService;
import com.taxflow.common.ApiResponse;
import com.taxflow.domain.model.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
public class AccountController {
    private final AccessService accessService;
    private final AuthService authService;

    @GetMapping("/me")
    public ApiResponse<AdminUserResponse> me() {
        User user = accessService.currentUser();
        return ApiResponse.ok(new AdminUserResponse(user.getId(), user.getEmail(), user.getFullName(), user.getRole(),
                user.isEnabled(), user.isEmailVerified(), user.getCreatedAt()));
    }

    @PutMapping("/password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request);
        return ApiResponse.message("Password changed", null);
    }
}
