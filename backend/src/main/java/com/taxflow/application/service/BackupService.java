package com.taxflow.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxflow.application.dto.TaxFlowDtos.CustomerRequest;
import com.taxflow.application.dto.TaxFlowDtos.ExpenseRequest;
import com.taxflow.application.dto.TaxFlowDtos.InvoiceRequest;
import com.taxflow.application.dto.TaxFlowDtos.ProductRequest;
import com.taxflow.domain.enums.TaskScope;
import com.taxflow.infrastructure.repository.CustomerRepository;
import com.taxflow.infrastructure.repository.DocumentRepository;
import com.taxflow.infrastructure.repository.ExpenseRepository;
import com.taxflow.infrastructure.repository.InvoiceRepository;
import com.taxflow.infrastructure.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BackupService {
    private final BusinessService businessService;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final InvoiceRepository invoiceRepository;
    private final ExpenseRepository expenseRepository;
    private final DocumentRepository documentRepository;
    private final CustomerService customerService;
    private final ProductService productService;
    private final InvoiceService invoiceService;
    private final ExpenseService expenseService;
    private final ObjectMapper objectMapper;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public Map<String, Object> export(UUID businessId) {
        businessService.business(businessId, TaskScope.SETTINGS);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("version", "1.0");
        data.put("businessId", businessId);
        data.put("customers", customerRepository.findByBusinessId(businessId, PageRequest.of(0, 500)).getContent());
        data.put("products", productRepository.findByBusinessId(businessId, PageRequest.of(0, 500)).getContent());
        data.put("invoices", invoiceRepository.findByBusinessId(businessId, PageRequest.of(0, 500)).getContent());
        data.put("expenses", expenseRepository.findByBusinessId(businessId, PageRequest.of(0, 500)).getContent());
        data.put("documents", documentRepository.findByBusinessId(businessId, PageRequest.of(0, 500)).getContent());
        return data;
    }

    @Transactional
    public Map<String, Integer> importData(UUID businessId, JsonNode payload) {
        businessService.business(businessId, TaskScope.SETTINGS);
        int customers = importArray(payload, "customers", node -> customerService.create(businessId, objectMapper.treeToValue(node, CustomerRequest.class)));
        int products = importArray(payload, "products", node -> productService.create(businessId, objectMapper.treeToValue(node, ProductRequest.class)));
        int expenses = importArray(payload, "expenses", node -> expenseService.create(businessId, objectMapper.treeToValue(node, ExpenseRequest.class)));
        int invoices = importArray(payload, "invoices", node -> invoiceService.create(businessId, objectMapper.treeToValue(node, InvoiceRequest.class)));
        auditService.log(businessId, "IMPORT", "BACKUP", businessId, null, "customers=" + customers + ",products=" + products + ",expenses=" + expenses + ",invoices=" + invoices);
        return Map.of("customers", customers, "products", products, "expenses", expenses, "invoices", invoices);
    }

    private int importArray(JsonNode payload, String name, ThrowingConsumer consumer) {
        JsonNode array = payload.get(name);
        if (array == null || !array.isArray()) {
            return 0;
        }
        int count = 0;
        for (JsonNode item : array) {
            try {
                consumer.accept(item);
                count++;
            } catch (Exception ignored) {
                // Invalid records are skipped so one bad backup row does not block the rest of a migration.
            }
        }
        return count;
    }

    @FunctionalInterface
    private interface ThrowingConsumer {
        void accept(JsonNode node) throws Exception;
    }
}
