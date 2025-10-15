package com.taxflow.application.service;

import com.taxflow.application.dto.TaxFlowDtos.AdminUserResponse;
import com.taxflow.application.dto.TaxFlowDtos.BusinessResponse;
import com.taxflow.application.mapper.BusinessMapper;
import com.taxflow.common.PageResponse;
import com.taxflow.domain.model.User;
import com.taxflow.infrastructure.repository.BusinessRepository;
import com.taxflow.infrastructure.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminService {
    private final UserRepository userRepository;
    private final BusinessRepository businessRepository;
    private final BusinessMapper businessMapper;

    @Transactional(readOnly = true)
    public PageResponse<AdminUserResponse> users(Pageable pageable) {
        return PageResponse.from(userRepository.findAll(pageable), this::toUserResponse);
    }

    @Transactional(readOnly = true)
    public PageResponse<BusinessResponse> businesses(Pageable pageable) {
        return PageResponse.from(businessRepository.findAll(pageable), businessMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> analytics() {
        return Map.of(
                "users", userRepository.count(),
                "businesses", businessRepository.count(),
                "monthlyRecurringRevenue", 0,
                "systemHealth", "GREEN"
        );
    }

    private AdminUserResponse toUserResponse(User user) {
        return new AdminUserResponse(user.getId(), user.getEmail(), user.getFullName(), user.getRole(),
                user.isEnabled(), user.isEmailVerified(), user.getCreatedAt());
    }
}
