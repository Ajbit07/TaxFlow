package com.taxflow.application.mapper;

import com.taxflow.application.dto.TaxFlowDtos.BusinessResponse;
import com.taxflow.domain.model.Business;
import org.springframework.stereotype.Component;

@Component
public class BusinessMapper {

    public BusinessResponse toResponse(Business business) {
        if (business == null) {
            return null;
        }
        return new BusinessResponse(
                business.getId(),
                business.getOwner().getId(),
                business.getGstin(),
                business.getPan(),
                business.getBusinessName(),
                business.getOwnerName(),
                business.getAddress(),
                business.getPhone(),
                business.getEmail(),
                business.getState(),
                business.getBusinessType(),
                business.getFinancialYear(),
                business.getCurrency(),
                business.getLanguage(),
                business.isDarkMode());
    }
}
