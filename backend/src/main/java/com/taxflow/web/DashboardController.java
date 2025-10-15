package com.taxflow.web;

import com.taxflow.application.dto.TaxFlowDtos.DashboardResponse;
import com.taxflow.application.service.TaxEngineService;
import com.taxflow.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/businesses/{businessId}/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    private final TaxEngineService taxEngineService;

    @GetMapping
    public ApiResponse<DashboardResponse> dashboard(@PathVariable UUID businessId) {
        return ApiResponse.ok(taxEngineService.dashboard(businessId));
    }
}
