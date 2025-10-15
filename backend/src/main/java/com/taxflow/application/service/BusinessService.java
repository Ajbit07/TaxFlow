package com.taxflow.application.service;

import com.taxflow.application.dto.TaxFlowDtos.AssignEmployeeRequest;
import com.taxflow.application.dto.TaxFlowDtos.BusinessRequest;
import com.taxflow.application.dto.TaxFlowDtos.BusinessResponse;
import com.taxflow.application.mapper.BusinessMapper;
import com.taxflow.common.InputSanitizer;
import com.taxflow.common.exception.NotFoundException;
import com.taxflow.domain.enums.TaskScope;
import com.taxflow.domain.enums.UserRole;
import com.taxflow.domain.model.Business;
import com.taxflow.domain.model.BusinessMember;
import com.taxflow.domain.model.User;
import com.taxflow.infrastructure.repository.BusinessMemberRepository;
import com.taxflow.infrastructure.repository.BusinessRepository;
import com.taxflow.infrastructure.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BusinessService {
    private final BusinessRepository businessRepository;
    private final BusinessMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final BusinessMapper mapper;
    private final AccessService accessService;
    private final InputSanitizer sanitizer;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<BusinessResponse> myBusinesses() {
        return businessRepository.findAccessible(accessService.currentUser().getId()).stream().map(mapper::toResponse).toList();
    }

    @Transactional
    public BusinessResponse create(BusinessRequest request) {
        User owner = accessService.currentUser();
        Business business = apply(Business.builder().owner(owner).build(), request);
        businessRepository.save(business);
        auditService.log(business.getId(), "CREATE", "BUSINESS", business.getId(), null, business.getBusinessName());
        return mapper.toResponse(business);
    }

    @Transactional
    public BusinessResponse update(UUID businessId, BusinessRequest request) {
        Business business = accessService.requireOwner(businessId);
        String old = business.getBusinessName();
        apply(business, request);
        auditService.log(businessId, "UPDATE", "BUSINESS", businessId, old, business.getBusinessName());
        return mapper.toResponse(business);
    }

    @Transactional
    public void assignEmployee(UUID businessId, AssignEmployeeRequest request) {
        Business business = accessService.requireOwner(businessId);
        User employee = userRepository.findByEmailIgnoreCase(request.email())
                .orElseGet(() -> userRepository.save(User.builder()
                        .email(request.email().toLowerCase())
                        .passwordHash(passwordEncoder.encode("TaxFlow@123"))
                        .fullName(request.email().split("@")[0])
                        .role(UserRole.EMPLOYEE)
                        .enabled(true)
                        .emailVerified(true)
                        .build()));
        BusinessMember member = memberRepository.findByBusinessIdAndUserId(businessId, employee.getId())
                .orElse(BusinessMember.builder().business(business).user(employee).build());
        member.setScopes(request.scopes());
        memberRepository.save(member);
        auditService.log(businessId, "ASSIGN_EMPLOYEE", "BUSINESS_MEMBER", employee.getId(), null, request.scopes().toString());
    }

    private Business apply(Business business, BusinessRequest request) {
        business.setGstin(sanitizer.clean(request.gstin()));
        business.setPan(sanitizer.clean(request.pan()));
        business.setBusinessName(sanitizer.clean(request.businessName()));
        business.setOwnerName(sanitizer.clean(request.ownerName()));
        business.setAddress(sanitizer.clean(request.address()));
        business.setPhone(sanitizer.clean(request.phone()));
        business.setEmail(sanitizer.clean(request.email()));
        business.setState(sanitizer.clean(request.state()));
        business.setBusinessType(request.businessType());
        business.setFinancialYear(sanitizer.clean(request.financialYear()));
        business.setCurrency(request.currency() == null ? "INR" : request.currency());
        business.setLanguage(request.language() == null ? "en" : request.language());
        business.setDarkMode(request.darkMode());
        return business;
    }

    public Business business(UUID businessId, TaskScope scope) {
        return accessService.requireBusiness(businessId, scope);
    }

    public Business get(UUID businessId) {
        return businessRepository.findById(businessId).orElseThrow(() -> new NotFoundException("Business not found"));
    }
}
