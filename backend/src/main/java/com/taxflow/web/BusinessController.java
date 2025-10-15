package com.taxflow.web;

import com.taxflow.application.dto.TaxFlowDtos.AssignEmployeeRequest;
import com.taxflow.application.dto.TaxFlowDtos.BusinessRequest;
import com.taxflow.application.dto.TaxFlowDtos.BusinessResponse;
import com.taxflow.application.service.BusinessService;
import com.taxflow.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/businesses")
@RequiredArgsConstructor
public class BusinessController {
    private final BusinessService businessService;

    @GetMapping
    public ApiResponse<List<BusinessResponse>> myBusinesses() {
        return ApiResponse.ok(businessService.myBusinesses());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<BusinessResponse> create(@Valid @RequestBody BusinessRequest request) {
        return ApiResponse.message("Business created", businessService.create(request));
    }

    @PutMapping("/{businessId}")
    public ApiResponse<BusinessResponse> update(@PathVariable UUID businessId, @Valid @RequestBody BusinessRequest request) {
        return ApiResponse.message("Business updated", businessService.update(businessId, request));
    }

    @PostMapping("/{businessId}/members")
    public ApiResponse<Void> assignEmployee(@PathVariable UUID businessId, @Valid @RequestBody AssignEmployeeRequest request) {
        businessService.assignEmployee(businessId, request);
        return ApiResponse.message("Employee assigned", null);
    }
}
