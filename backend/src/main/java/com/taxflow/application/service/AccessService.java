package com.taxflow.application.service;

import com.taxflow.common.exception.NotFoundException;
import com.taxflow.domain.enums.TaskScope;
import com.taxflow.domain.enums.UserRole;
import com.taxflow.domain.model.Business;
import com.taxflow.domain.model.BusinessMember;
import com.taxflow.domain.model.User;
import com.taxflow.infrastructure.repository.BusinessMemberRepository;
import com.taxflow.infrastructure.repository.BusinessRepository;
import com.taxflow.infrastructure.repository.UserRepository;
import com.taxflow.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccessService {
    private final UserRepository userRepository;
    private final BusinessRepository businessRepository;
    private final BusinessMemberRepository memberRepository;

    public User currentUser() {
        return userRepository.findById(SecurityUtils.userId()).orElseThrow(() -> new NotFoundException("User not found"));
    }

    public Business requireBusiness(UUID businessId, TaskScope scope) {
        User user = currentUser();
        Business business = businessRepository.findById(businessId).orElseThrow(() -> new NotFoundException("Business not found"));
        if (user.getRole() == UserRole.ADMIN || business.getOwner().getId().equals(user.getId())) {
            return business;
        }
        BusinessMember member = memberRepository.findByBusinessIdAndUserId(businessId, user.getId())
                .orElseThrow(() -> new AccessDeniedException("Business access denied"));
        if (!member.getScopes().contains(scope)) {
            throw new AccessDeniedException("Employee is not assigned to " + scope);
        }
        return business;
    }

    public Business requireOwner(UUID businessId) {
        User user = currentUser();
        Business business = businessRepository.findById(businessId).orElseThrow(() -> new NotFoundException("Business not found"));
        if (user.getRole() != UserRole.ADMIN && !business.getOwner().getId().equals(user.getId())) {
            throw new AccessDeniedException("Owner access required");
        }
        return business;
    }
}
