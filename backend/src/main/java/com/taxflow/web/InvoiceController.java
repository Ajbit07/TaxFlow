package com.taxflow.web;

import com.taxflow.application.dto.TaxFlowDtos.InvoiceRequest;
import com.taxflow.application.dto.TaxFlowDtos.InvoiceResponse;
import com.taxflow.application.service.InvoicePdfService;
import com.taxflow.application.service.InvoiceService;
import com.taxflow.common.ApiResponse;
import com.taxflow.common.PageResponse;
import com.taxflow.domain.enums.InvoiceStatus;
import com.taxflow.domain.enums.InvoiceType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/businesses/{businessId}/invoices")
@RequiredArgsConstructor
public class InvoiceController {
    private final InvoiceService invoiceService;
    private final InvoicePdfService invoicePdfService;

    @GetMapping
    public ApiResponse<PageResponse<InvoiceResponse>> list(@PathVariable UUID businessId,
                                                           @RequestParam(required = false) String query,
                                                           @RequestParam(required = false) InvoiceStatus status,
                                                           @RequestParam(required = false) InvoiceType type,
                                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                                           @PageableDefault(size = 20, sort = "invoiceDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.ok(invoiceService.list(businessId, query, status, type, from, to, pageable));
    }

    @GetMapping("/next-number")
    public ApiResponse<Map<String, String>> nextNumber(@PathVariable UUID businessId) {
        return ApiResponse.ok(Map.of("invoiceNumber", invoiceService.nextNumber(businessId)));
    }

    @GetMapping("/{invoiceId}")
    public ApiResponse<InvoiceResponse> preview(@PathVariable UUID businessId, @PathVariable UUID invoiceId) {
        return ApiResponse.ok(invoiceService.preview(businessId, invoiceId));
    }

    @GetMapping("/{invoiceId}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable UUID businessId, @PathVariable UUID invoiceId) {
        byte[] bytes = invoicePdfService.generate(businessId, invoiceId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=invoice-" + invoiceId + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(bytes);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<InvoiceResponse> create(@PathVariable UUID businessId, @Valid @RequestBody InvoiceRequest request) {
        return ApiResponse.message("Invoice created", invoiceService.create(businessId, request));
    }

    @PutMapping("/{invoiceId}")
    public ApiResponse<InvoiceResponse> update(@PathVariable UUID businessId, @PathVariable UUID invoiceId,
                                               @Valid @RequestBody InvoiceRequest request) {
        return ApiResponse.message("Invoice updated", invoiceService.update(businessId, invoiceId, request));
    }

    @PostMapping("/{invoiceId}/duplicate")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<InvoiceResponse> duplicate(@PathVariable UUID businessId, @PathVariable UUID invoiceId) {
        return ApiResponse.message("Invoice duplicated", invoiceService.duplicate(businessId, invoiceId));
    }

    @DeleteMapping("/{invoiceId}")
    public ApiResponse<Void> delete(@PathVariable UUID businessId, @PathVariable UUID invoiceId) {
        invoiceService.delete(businessId, invoiceId);
        return ApiResponse.message("Invoice deleted", null);
    }
}
