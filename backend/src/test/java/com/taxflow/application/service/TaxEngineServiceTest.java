package com.taxflow.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxflow.application.dto.TaxFlowDtos.IncomeTaxRequest;
import com.taxflow.application.dto.TaxFlowDtos.IncomeTaxResponse;
import com.taxflow.infrastructure.repository.BusinessRuleRepository;
import com.taxflow.infrastructure.repository.CustomerRepository;
import com.taxflow.infrastructure.repository.DocumentRepository;
import com.taxflow.infrastructure.repository.ExpenseRepository;
import com.taxflow.infrastructure.repository.InvoiceRepository;
import com.taxflow.infrastructure.repository.NotificationRepository;
import com.taxflow.infrastructure.repository.ProductRepository;
import com.taxflow.infrastructure.repository.TaxFilingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class TaxEngineServiceTest {

    private TaxEngineService service;

    @BeforeEach
    void setUp() {
        service = new TaxEngineService(
                Mockito.mock(BusinessService.class),
                Mockito.mock(InvoiceRepository.class),
                Mockito.mock(ExpenseRepository.class),
                Mockito.mock(ProductRepository.class),
                Mockito.mock(CustomerRepository.class),
                Mockito.mock(DocumentRepository.class),
                Mockito.mock(NotificationRepository.class),
                Mockito.mock(TaxFilingRepository.class),
                Mockito.mock(BusinessRuleRepository.class),
                Mockito.mock(NotificationService.class),
                new ObjectMapper(),
                Mockito.mock(AuditService.class));
    }

    @Test
    void incomeBelowBasicExemptionHasZeroTax() {
        IncomeTaxResponse response = service.incomeTax(request(200000, 0, 0, 0, 0, 0));
        assertThat(response.estimatedTax()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.taxableIncome()).isEqualByComparingTo(BigDecimal.valueOf(200000));
    }

    @Test
    void fivePercentSlabAppliedBetween250kAnd500k() {
        IncomeTaxResponse response = service.incomeTax(request(400000, 0, 0, 0, 0, 0));
        // (400000 - 250000) * 5% = 7500
        assertThat(response.estimatedTax()).isEqualByComparingTo(BigDecimal.valueOf(7500.0));
    }

    @Test
    void thirtyPercentSlabAppliedAboveTenLakh() {
        IncomeTaxResponse response = service.incomeTax(request(1500000, 0, 0, 0, 0, 0));
        // 112500 + (1500000 - 1000000) * 30% = 262500
        assertThat(response.estimatedTax()).isEqualByComparingTo(BigDecimal.valueOf(262500.0));
        assertThat(response.advanceTaxDue()).isEqualByComparingTo(BigDecimal.valueOf(65625.00));
    }

    @Test
    void investmentsAreCappedAt80CLimit() {
        // 300000 invested but only 150000 deductible: taxable = 1000000 - 150000 = 850000
        IncomeTaxResponse response = service.incomeTax(request(1000000, 0, 0, 300000, 0, 0));
        assertThat(response.taxableIncome()).isEqualByComparingTo(BigDecimal.valueOf(850000));
    }

    @Test
    void businessExpensesReduceTaxableIncomeAndAppearInPnl() {
        IncomeTaxResponse response = service.incomeTax(request(800000, 0, 0, 0, 0, 200000));
        assertThat(response.taxableIncome()).isEqualByComparingTo(BigDecimal.valueOf(600000));
        assertThat(response.profitAndLoss().get("netProfit")).isEqualByComparingTo(BigDecimal.valueOf(600000));
    }

    @Test
    void suggestionsIncludeRemaining80CHeadroom() {
        IncomeTaxResponse response = service.incomeTax(request(1000000, 0, 0, 50000, 0, 0));
        assertThat(response.taxSavingSuggestions())
                .anyMatch(suggestion -> suggestion.contains("80C"));
    }

    private IncomeTaxRequest request(double business, double salary, double other,
                                     double investments, double deductions, double expenses) {
        return new IncomeTaxRequest(
                BigDecimal.valueOf(business), BigDecimal.valueOf(salary), BigDecimal.valueOf(other),
                BigDecimal.valueOf(investments), BigDecimal.valueOf(deductions), BigDecimal.valueOf(expenses));
    }
}
