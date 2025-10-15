package com.taxflow.application.service;

import com.taxflow.application.dto.TaxFlowDtos.InvoiceLineRequest;
import com.taxflow.application.dto.TaxFlowDtos.InvoiceLineResponse;
import com.taxflow.application.dto.TaxFlowDtos.InvoiceRequest;
import com.taxflow.application.dto.TaxFlowDtos.InvoiceResponse;
import com.taxflow.common.InputSanitizer;
import com.taxflow.common.PageResponse;
import com.taxflow.common.exception.NotFoundException;
import com.taxflow.domain.enums.InvoiceStatus;
import com.taxflow.domain.enums.InvoiceType;
import com.taxflow.domain.enums.TaskScope;
import com.taxflow.domain.model.Business;
import com.taxflow.domain.model.Customer;
import com.taxflow.domain.model.Invoice;
import com.taxflow.domain.model.InvoiceLine;
import com.taxflow.domain.model.Product;
import com.taxflow.infrastructure.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InvoiceService {
    private final InvoiceRepository invoiceRepository;
    private final BusinessService businessService;
    private final CustomerService customerService;
    private final ProductService productService;
    private final InputSanitizer sanitizer;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public PageResponse<InvoiceResponse> list(UUID businessId, String query, InvoiceStatus status, InvoiceType type,
                                              LocalDate from, LocalDate to, Pageable pageable) {
        businessService.business(businessId, TaskScope.INVOICE);
        Specification<Invoice> spec = (root, cq, cb) -> cb.equal(root.get("business").get("id"), businessId);
        if (query != null && !query.isBlank()) {
            String like = "%" + query.toLowerCase() + "%";
            spec = spec.and((root, cq, cb) -> cb.like(cb.lower(root.get("invoiceNumber")), like));
        }
        if (status != null) {
            spec = spec.and((root, cq, cb) -> cb.equal(root.get("status"), status));
        }
        if (type != null) {
            spec = spec.and((root, cq, cb) -> cb.equal(root.get("type"), type));
        }
        if (from != null) {
            spec = spec.and((root, cq, cb) -> cb.greaterThanOrEqualTo(root.get("invoiceDate"), from));
        }
        if (to != null) {
            spec = spec.and((root, cq, cb) -> cb.lessThanOrEqualTo(root.get("invoiceDate"), to));
        }
        return PageResponse.from(invoiceRepository.findAll(spec, pageable), this::toResponse);
    }

    @Transactional
    public InvoiceResponse create(UUID businessId, InvoiceRequest request) {
        Business business = businessService.business(businessId, TaskScope.INVOICE);
        Invoice invoice = Invoice.builder().business(business).lines(new ArrayList<>()).build();
        apply(invoice, request, true);
        invoiceRepository.save(invoice);
        applyStock(invoice, BigDecimal.ONE);
        auditService.log(businessId, "CREATE", "INVOICE", invoice.getId(), null, invoice.getInvoiceNumber());
        return toResponse(invoice);
    }

    @Transactional
    public InvoiceResponse update(UUID businessId, UUID invoiceId, InvoiceRequest request) {
        businessService.business(businessId, TaskScope.INVOICE);
        Invoice invoice = require(businessId, invoiceId);
        applyStock(invoice, BigDecimal.ONE.negate());
        String old = invoice.getInvoiceNumber();
        invoice.getLines().clear();
        apply(invoice, request, false);
        applyStock(invoice, BigDecimal.ONE);
        auditService.log(businessId, "UPDATE", "INVOICE", invoiceId, old, invoice.getInvoiceNumber());
        return toResponse(invoice);
    }

    @Transactional
    public InvoiceResponse duplicate(UUID businessId, UUID invoiceId) {
        Invoice source = require(businessId, invoiceId);
        Invoice copy = Invoice.builder()
                .business(source.getBusiness())
                .customer(source.getCustomer())
                .invoiceNumber(nextNumber(businessId))
                .type(source.getType())
                .status(InvoiceStatus.DRAFT)
                .invoiceDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(15))
                .recurring(source.isRecurring())
                .recurrencePattern(source.getRecurrencePattern())
                .templateName(source.getTemplateName())
                .paidAmount(BigDecimal.ZERO)
                .notes(source.getNotes())
                .lines(new ArrayList<>())
                .build();
        for (InvoiceLine line : source.getLines()) {
            InvoiceLine copyLine = InvoiceLine.builder()
                    .invoice(copy)
                    .product(line.getProduct())
                    .description(line.getDescription())
                    .hsnCode(line.getHsnCode())
                    .quantity(line.getQuantity())
                    .unitPrice(line.getUnitPrice())
                    .gstRate(line.getGstRate())
                    .taxableAmount(line.getTaxableAmount())
                    .gstAmount(line.getGstAmount())
                    .totalAmount(line.getTotalAmount())
                    .build();
            copy.getLines().add(copyLine);
        }
        recalculate(copy);
        invoiceRepository.save(copy);
        auditService.log(businessId, "DUPLICATE", "INVOICE", invoiceId, source.getInvoiceNumber(), copy.getInvoiceNumber());
        return toResponse(copy);
    }

    @Transactional
    public void delete(UUID businessId, UUID invoiceId) {
        businessService.business(businessId, TaskScope.INVOICE);
        Invoice invoice = require(businessId, invoiceId);
        applyStock(invoice, BigDecimal.ONE.negate());
        invoiceRepository.delete(invoice);
        auditService.log(businessId, "DELETE", "INVOICE", invoiceId, invoice.getInvoiceNumber(), null);
    }

    @Transactional(readOnly = true)
    public InvoiceResponse preview(UUID businessId, UUID invoiceId) {
        businessService.business(businessId, TaskScope.INVOICE);
        return toResponse(require(businessId, invoiceId));
    }

    public String nextNumber(UUID businessId) {
        long count = invoiceRepository.countByBusinessIdAndType(businessId, InvoiceType.GST_INVOICE) + 1;
        return "TF-" + LocalDate.now().getYear() + "-" + String.format("%05d", count);
    }

    public Invoice require(UUID businessId, UUID invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId).orElseThrow(() -> new NotFoundException("Invoice not found"));
        if (!invoice.getBusiness().getId().equals(businessId)) {
            throw new NotFoundException("Invoice not found");
        }
        return invoice;
    }

    private void apply(Invoice invoice, InvoiceRequest request, boolean creating) {
        if (request.customerId() != null) {
            Customer customer = customerService.require(invoice.getBusiness().getId(), request.customerId());
            invoice.setCustomer(customer);
        }
        String number = request.invoiceNumber() == null || request.invoiceNumber().isBlank()
                ? nextNumber(invoice.getBusiness().getId())
                : sanitizer.clean(request.invoiceNumber());
        if (creating && invoiceRepository.existsByBusinessIdAndInvoiceNumber(invoice.getBusiness().getId(), number)) {
            number = nextNumber(invoice.getBusiness().getId());
        }
        invoice.setInvoiceNumber(number);
        invoice.setType(request.type());
        invoice.setStatus(request.status());
        invoice.setInvoiceDate(request.invoiceDate());
        invoice.setDueDate(request.dueDate());
        invoice.setRecurring(request.recurring());
        invoice.setRecurrencePattern(sanitizer.clean(request.recurrencePattern()));
        invoice.setTemplateName(request.templateName() == null ? "Classic GST" : sanitizer.clean(request.templateName()));
        invoice.setPaidAmount(request.paidAmount() == null ? BigDecimal.ZERO : request.paidAmount());
        invoice.setNotes(sanitizer.clean(request.notes()));
        for (InvoiceLineRequest lineRequest : request.lines()) {
            Product product = lineRequest.productId() == null ? null : productService.require(invoice.getBusiness().getId(), lineRequest.productId());
            InvoiceLine line = buildLine(invoice, product, lineRequest);
            invoice.getLines().add(line);
        }
        recalculate(invoice);
        invoice.setQrPayload("upi://pay?pa=taxflow@" + invoice.getBusiness().getBusinessName().replaceAll("\\s+", "").toLowerCase()
                + "&am=" + invoice.getTotalAmount() + "&tn=" + invoice.getInvoiceNumber());
    }

    private InvoiceLine buildLine(Invoice invoice, Product product, InvoiceLineRequest request) {
        BigDecimal taxable = request.quantity().multiply(request.unitPrice()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal gst = taxable.multiply(request.gstRate()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        return InvoiceLine.builder()
                .invoice(invoice)
                .product(product)
                .description(sanitizer.clean(request.description()))
                .hsnCode(sanitizer.clean(request.hsnCode()))
                .quantity(request.quantity())
                .unitPrice(request.unitPrice())
                .gstRate(request.gstRate())
                .taxableAmount(taxable)
                .gstAmount(gst)
                .totalAmount(taxable.add(gst))
                .build();
    }

    private void recalculate(Invoice invoice) {
        BigDecimal subtotal = invoice.getLines().stream().map(InvoiceLine::getTaxableAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal gst = invoice.getLines().stream().map(InvoiceLine::getGstAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        invoice.setSubtotal(subtotal);
        invoice.setTotalGst(gst);
        invoice.setTotalAmount(subtotal.add(gst));
    }

    private void applyStock(Invoice invoice, BigDecimal multiplier) {
        BigDecimal direction = switch (invoice.getType()) {
            case GST_INVOICE, DEBIT_NOTE -> BigDecimal.ONE.negate();
            case PURCHASE_BILL, CREDIT_NOTE -> BigDecimal.ONE;
        };
        for (InvoiceLine line : invoice.getLines()) {
            if (line.getProduct() != null) {
                productService.changeStock(line.getProduct(), line.getQuantity().multiply(direction).multiply(multiplier));
            }
        }
    }

    public InvoiceResponse toResponse(Invoice invoice) {
        return new InvoiceResponse(invoice.getId(), invoice.getBusiness().getId(),
                invoice.getCustomer() == null ? null : invoice.getCustomer().getId(),
                invoice.getCustomer() == null ? "Walk-in customer" : invoice.getCustomer().getName(),
                invoice.getInvoiceNumber(), invoice.getType(), invoice.getStatus(), invoice.getInvoiceDate(), invoice.getDueDate(),
                invoice.isRecurring(), invoice.getRecurrencePattern(), invoice.getTemplateName(), invoice.getSubtotal(),
                invoice.getTotalGst(), invoice.getTotalAmount(), invoice.getPaidAmount(), invoice.getNotes(),
                invoice.getQrPayload(), invoice.getLines().stream().map(this::toLineResponse).toList(), invoice.getCreatedAt());
    }

    private InvoiceLineResponse toLineResponse(InvoiceLine line) {
        return new InvoiceLineResponse(line.getId(), line.getProduct() == null ? null : line.getProduct().getId(),
                line.getDescription(), line.getHsnCode(), line.getQuantity(), line.getUnitPrice(), line.getGstRate(),
                line.getTaxableAmount(), line.getGstAmount(), line.getTotalAmount());
    }
}
