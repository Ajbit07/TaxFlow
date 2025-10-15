package com.taxflow.web;

import com.taxflow.application.dto.TaxFlowDtos.AuditResponse;
import com.taxflow.application.service.AuditService;
import com.taxflow.application.service.BusinessService;
import com.taxflow.common.ApiResponse;
import com.taxflow.common.PageResponse;
import com.taxflow.domain.enums.TaskScope;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/businesses/{businessId}/audit")
@RequiredArgsConstructor
public class AuditController {
    private final AuditService auditService;
    private final BusinessService businessService;

    @GetMapping
    public ApiResponse<PageResponse<AuditResponse>> list(@PathVariable UUID businessId,
                                                         @PageableDefault(size = 25, sort = "actionTime", direction = Sort.Direction.DESC) Pageable pageable) {
        businessService.business(businessId, TaskScope.SETTINGS);
        return ApiResponse.ok(auditService.list(businessId, pageable));
    }
}
