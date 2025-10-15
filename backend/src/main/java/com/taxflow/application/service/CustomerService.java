package com.taxflow.application.service;

import com.taxflow.application.dto.TaxFlowDtos.CustomerRequest;
import com.taxflow.application.dto.TaxFlowDtos.CustomerResponse;
import com.taxflow.application.mapper.CustomerMapper;
import com.taxflow.common.InputSanitizer;
import com.taxflow.common.PageResponse;
import com.taxflow.common.exception.NotFoundException;
import com.taxflow.domain.enums.TaskScope;
import com.taxflow.domain.model.Business;
import com.taxflow.domain.model.Customer;
import com.taxflow.infrastructure.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomerService {
    private final CustomerRepository customerRepository;
    private final BusinessService businessService;
    private final CustomerMapper mapper;
    private final InputSanitizer sanitizer;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public PageResponse<CustomerResponse> list(UUID businessId, String query, Pageable pageable) {
        businessService.business(businessId, TaskScope.CUSTOMER);
        Specification<Customer> spec = (root, cq, cb) -> cb.equal(root.get("business").get("id"), businessId);
        if (query != null && !query.isBlank()) {
            String like = "%" + query.toLowerCase() + "%";
            spec = spec.and((root, cq, cb) -> cb.or(
                    cb.like(cb.lower(root.get("name")), like),
                    cb.like(cb.lower(root.get("gstin")), like),
                    cb.like(cb.lower(root.get("pan")), like)));
        }
        return PageResponse.from(customerRepository.findAll(spec, pageable), mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> search(UUID businessId, String query) {
        businessService.business(businessId, TaskScope.CUSTOMER);
        return customerRepository.findTop10ByBusinessIdAndNameContainingIgnoreCaseOrderByName(businessId, query)
                .stream().map(mapper::toResponse).toList();
    }

    @Transactional
    public CustomerResponse create(UUID businessId, CustomerRequest request) {
        Business business = businessService.business(businessId, TaskScope.CUSTOMER);
        Customer customer = apply(Customer.builder().business(business).build(), request);
        customerRepository.save(customer);
        auditService.log(businessId, "CREATE", "CUSTOMER", customer.getId(), null, customer.getName());
        return mapper.toResponse(customer);
    }

    @Transactional
    public CustomerResponse update(UUID businessId, UUID customerId, CustomerRequest request) {
        businessService.business(businessId, TaskScope.CUSTOMER);
        Customer customer = require(businessId, customerId);
        String old = customer.getName();
        apply(customer, request);
        auditService.log(businessId, "UPDATE", "CUSTOMER", customerId, old, customer.getName());
        return mapper.toResponse(customer);
    }

    @Transactional
    public void delete(UUID businessId, UUID customerId) {
        businessService.business(businessId, TaskScope.CUSTOMER);
        Customer customer = require(businessId, customerId);
        customerRepository.delete(customer);
        auditService.log(businessId, "DELETE", "CUSTOMER", customerId, customer.getName(), null);
    }

    public Customer require(UUID businessId, UUID customerId) {
        Customer customer = customerRepository.findById(customerId).orElseThrow(() -> new NotFoundException("Customer not found"));
        if (!customer.getBusiness().getId().equals(businessId)) {
            throw new NotFoundException("Customer not found");
        }
        return customer;
    }

    private Customer apply(Customer customer, CustomerRequest request) {
        customer.setName(sanitizer.clean(request.name()));
        customer.setGstin(sanitizer.clean(request.gstin()));
        customer.setPan(sanitizer.clean(request.pan()));
        customer.setPhone(sanitizer.clean(request.phone()));
        customer.setEmail(sanitizer.clean(request.email()));
        customer.setAddress(sanitizer.clean(request.address()));
        customer.setOutstandingBalance(request.openingBalance() == null ? BigDecimal.ZERO : request.openingBalance());
        return customer;
    }
}
