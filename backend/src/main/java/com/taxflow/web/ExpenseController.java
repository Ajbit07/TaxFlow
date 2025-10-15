package com.taxflow.web;

import com.taxflow.application.dto.TaxFlowDtos.ExpenseRequest;
import com.taxflow.application.dto.TaxFlowDtos.ExpenseResponse;
import com.taxflow.application.service.ExpenseService;
import com.taxflow.common.ApiResponse;
import com.taxflow.common.PageResponse;
import com.taxflow.domain.enums.ExpenseCategory;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/businesses/{businessId}/expenses")
@RequiredArgsConstructor
public class ExpenseController {
    private final ExpenseService expenseService;

    @GetMapping
    public ApiResponse<PageResponse<ExpenseResponse>> list(@PathVariable UUID businessId,
                                                           @RequestParam(required = false) ExpenseCategory category,
                                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                                           @PageableDefault(size = 20, sort = "expenseDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.ok(expenseService.list(businessId, category, from, to, pageable));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ExpenseResponse> create(@PathVariable UUID businessId, @Valid @RequestBody ExpenseRequest request) {
        return ApiResponse.message("Expense recorded", expenseService.create(businessId, request));
    }

    @PutMapping("/{expenseId}")
    public ApiResponse<ExpenseResponse> update(@PathVariable UUID businessId, @PathVariable UUID expenseId,
                                               @Valid @RequestBody ExpenseRequest request) {
        return ApiResponse.message("Expense updated", expenseService.update(businessId, expenseId, request));
    }

    @DeleteMapping("/{expenseId}")
    public ApiResponse<Void> delete(@PathVariable UUID businessId, @PathVariable UUID expenseId) {
        expenseService.delete(businessId, expenseId);
        return ApiResponse.message("Expense deleted", null);
    }
}
