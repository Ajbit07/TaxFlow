package com.taxflow.application.service;

import com.taxflow.application.dto.TaxFlowDtos.ExpenseRequest;
import com.taxflow.application.dto.TaxFlowDtos.ExpenseResponse;
import com.taxflow.common.InputSanitizer;
import com.taxflow.common.PageResponse;
import com.taxflow.common.exception.NotFoundException;
import com.taxflow.domain.enums.ExpenseCategory;
import com.taxflow.domain.enums.TaskScope;
import com.taxflow.domain.model.Business;
import com.taxflow.domain.model.Expense;
import com.taxflow.infrastructure.repository.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExpenseService {
    private final ExpenseRepository expenseRepository;
    private final BusinessService businessService;
    private final InputSanitizer sanitizer;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public PageResponse<ExpenseResponse> list(UUID businessId, ExpenseCategory category, LocalDate from, LocalDate to, Pageable pageable) {
        businessService.business(businessId, TaskScope.EXPENSE);
        Specification<Expense> spec = (root, cq, cb) -> cb.equal(root.get("business").get("id"), businessId);
        if (category != null) {
            spec = spec.and((root, cq, cb) -> cb.equal(root.get("category"), category));
        }
        if (from != null) {
            spec = spec.and((root, cq, cb) -> cb.greaterThanOrEqualTo(root.get("expenseDate"), from));
        }
        if (to != null) {
            spec = spec.and((root, cq, cb) -> cb.lessThanOrEqualTo(root.get("expenseDate"), to));
        }
        return PageResponse.from(expenseRepository.findAll(spec, pageable), this::toResponse);
    }

    @Transactional
    public ExpenseResponse create(UUID businessId, ExpenseRequest request) {
        Business business = businessService.business(businessId, TaskScope.EXPENSE);
        Expense expense = apply(Expense.builder().business(business).build(), request);
        expenseRepository.save(expense);
        auditService.log(businessId, "CREATE", "EXPENSE", expense.getId(), null, expense.getAmount().toString());
        return toResponse(expense);
    }

    @Transactional
    public ExpenseResponse update(UUID businessId, UUID expenseId, ExpenseRequest request) {
        businessService.business(businessId, TaskScope.EXPENSE);
        Expense expense = require(businessId, expenseId);
        String old = expense.getAmount().toString();
        apply(expense, request);
        auditService.log(businessId, "UPDATE", "EXPENSE", expenseId, old, expense.getAmount().toString());
        return toResponse(expense);
    }

    @Transactional
    public void delete(UUID businessId, UUID expenseId) {
        businessService.business(businessId, TaskScope.EXPENSE);
        Expense expense = require(businessId, expenseId);
        expenseRepository.delete(expense);
        auditService.log(businessId, "DELETE", "EXPENSE", expenseId, expense.getAmount().toString(), null);
    }

    private Expense require(UUID businessId, UUID expenseId) {
        Expense expense = expenseRepository.findById(expenseId).orElseThrow(() -> new NotFoundException("Expense not found"));
        if (!expense.getBusiness().getId().equals(businessId)) {
            throw new NotFoundException("Expense not found");
        }
        return expense;
    }

    private Expense apply(Expense expense, ExpenseRequest request) {
        expense.setCategory(request.category());
        expense.setVendor(sanitizer.clean(request.vendor()));
        expense.setAmount(request.amount());
        expense.setGstAmount(request.gstAmount());
        expense.setExpenseDate(request.expenseDate());
        expense.setReceiptUrl(sanitizer.clean(request.receiptUrl()));
        expense.setDescription(sanitizer.clean(request.description()));
        return expense;
    }

    private ExpenseResponse toResponse(Expense expense) {
        return new ExpenseResponse(expense.getId(), expense.getCategory(), expense.getVendor(), expense.getAmount(),
                expense.getGstAmount(), expense.getExpenseDate(), expense.getReceiptUrl(), expense.getDescription());
    }
}
