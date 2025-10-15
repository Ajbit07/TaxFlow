package com.taxflow.application.service;

import com.taxflow.application.dto.TaxFlowDtos.SearchResult;
import com.taxflow.domain.enums.TaskScope;
import com.taxflow.infrastructure.repository.CustomerRepository;
import com.taxflow.infrastructure.repository.DocumentRepository;
import com.taxflow.infrastructure.repository.InvoiceRepository;
import com.taxflow.infrastructure.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SearchService {
    private final BusinessService businessService;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final InvoiceRepository invoiceRepository;
    private final DocumentRepository documentRepository;

    @Transactional(readOnly = true)
    public List<SearchResult> global(UUID businessId, String query) {
        businessService.business(businessId, TaskScope.DASHBOARD);
        String q = query == null ? "" : query;
        List<SearchResult> results = new ArrayList<>();
        customerRepository.findTop10ByBusinessIdAndNameContainingIgnoreCaseOrderByName(businessId, q)
                .forEach(c -> results.add(new SearchResult("CUSTOMER", c.getId(), c.getName(), nvl(c.getGstin(), c.getPan()), "/customers/" + c.getId())));
        productRepository.findTop10ByBusinessIdAndNameContainingIgnoreCaseOrderByName(businessId, q)
                .forEach(p -> results.add(new SearchResult("PRODUCT", p.getId(), p.getName(), p.getSku() + " / " + p.getHsnCode(), "/products/" + p.getId())));
        invoiceRepository.findTop10ByBusinessIdOrderByCreatedAtDesc(businessId).stream()
                .filter(i -> i.getInvoiceNumber().toLowerCase().contains(q.toLowerCase()))
                .forEach(i -> results.add(new SearchResult("INVOICE", i.getId(), i.getInvoiceNumber(), i.getStatus().name(), "/invoices/" + i.getId())));
        documentRepository.findByBusinessId(businessId, PageRequest.of(0, 10)).stream()
                .filter(d -> d.getFileName().toLowerCase().contains(q.toLowerCase()))
                .forEach(d -> results.add(new SearchResult("DOCUMENT", d.getId(), d.getFileName(), d.getDocumentType().name(), "/documents/" + d.getId())));
        return results.stream().limit(20).toList();
    }

    private String nvl(String first, String second) {
        return first == null || first.isBlank() ? second : first;
    }
}
