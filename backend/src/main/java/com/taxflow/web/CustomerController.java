package com.taxflow.web;

import com.taxflow.application.dto.TaxFlowDtos.CustomerRequest;
import com.taxflow.application.dto.TaxFlowDtos.CustomerResponse;
import com.taxflow.application.service.CustomerService;
import com.taxflow.common.ApiResponse;
import com.taxflow.common.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/businesses/{businessId}/customers")
@RequiredArgsConstructor
public class CustomerController {
    private final CustomerService customerService;

    @GetMapping
    public ApiResponse<PageResponse<CustomerResponse>> list(@PathVariable UUID businessId,
                                                            @RequestParam(required = false) String query,
                                                            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ApiResponse.ok(customerService.list(businessId, query, pageable));
    }

    @GetMapping("/search")
    public ApiResponse<List<CustomerResponse>> search(@PathVariable UUID businessId, @RequestParam String query) {
        return ApiResponse.ok(customerService.search(businessId, query));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CustomerResponse> create(@PathVariable UUID businessId, @Valid @RequestBody CustomerRequest request) {
        return ApiResponse.message("Customer created", customerService.create(businessId, request));
    }

    @PutMapping("/{customerId}")
    public ApiResponse<CustomerResponse> update(@PathVariable UUID businessId, @PathVariable UUID customerId,
                                                @Valid @RequestBody CustomerRequest request) {
        return ApiResponse.message("Customer updated", customerService.update(businessId, customerId, request));
    }

    @DeleteMapping("/{customerId}")
    public ApiResponse<Void> delete(@PathVariable UUID businessId, @PathVariable UUID customerId) {
        customerService.delete(businessId, customerId);
        return ApiResponse.message("Customer deleted", null);
    }
}
