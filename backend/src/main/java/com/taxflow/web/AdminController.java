package com.taxflow.web;

import com.taxflow.application.dto.TaxFlowDtos.AdminUserResponse;
import com.taxflow.application.dto.TaxFlowDtos.BusinessResponse;
import com.taxflow.application.dto.TaxFlowDtos.RuleRequest;
import com.taxflow.application.dto.TaxFlowDtos.RuleResponse;
import com.taxflow.application.service.AdminService;
import com.taxflow.application.service.TaxEngineService;
import com.taxflow.common.ApiResponse;
import com.taxflow.common.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    private final AdminService adminService;
    private final TaxEngineService taxEngineService;

    @GetMapping("/users")
    public ApiResponse<PageResponse<AdminUserResponse>> users(
            @PageableDefault(size = 25, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.ok(adminService.users(pageable));
    }

    @GetMapping("/businesses")
    public ApiResponse<PageResponse<BusinessResponse>> businesses(
            @PageableDefault(size = 25, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.ok(adminService.businesses(pageable));
    }

    @GetMapping("/analytics")
    public ApiResponse<Map<String, Object>> analytics() {
        return ApiResponse.ok(adminService.analytics());
    }

    @GetMapping("/rules")
    public ApiResponse<List<RuleResponse>> rules() {
        return ApiResponse.ok(taxEngineService.rules());
    }

    @PostMapping("/rules")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RuleResponse> createRule(@Valid @RequestBody RuleRequest request) {
        return ApiResponse.message("Rule created", taxEngineService.createRule(request));
    }
}
