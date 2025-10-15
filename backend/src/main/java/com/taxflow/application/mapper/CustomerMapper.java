package com.taxflow.application.mapper;

import com.taxflow.application.dto.TaxFlowDtos.CustomerResponse;
import com.taxflow.domain.model.Customer;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class CustomerMapper {

    public CustomerResponse toResponse(Customer customer) {
        if (customer == null) {
            return null;
        }
        return new CustomerResponse(
                customer.getId(),
                customer.getName(),
                customer.getGstin(),
                customer.getPan(),
                customer.getPhone(),
                customer.getEmail(),
                customer.getAddress(),
                customer.getOutstandingBalance(),
                BigDecimal.ZERO,
                0L);
    }
}
