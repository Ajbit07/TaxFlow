package com.taxflow.infrastructure.repository;

import com.taxflow.domain.enums.InvoiceStatus;
import com.taxflow.domain.enums.InvoiceType;
import com.taxflow.domain.model.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID>, JpaSpecificationExecutor<Invoice> {
    Page<Invoice> findByBusinessId(UUID businessId, Pageable pageable);
    Optional<Invoice> findByBusinessIdAndInvoiceNumber(UUID businessId, String invoiceNumber);
    boolean existsByBusinessIdAndInvoiceNumber(UUID businessId, String invoiceNumber);
    long countByBusinessIdAndStatus(UUID businessId, InvoiceStatus status);
    long countByBusinessIdAndType(UUID businessId, InvoiceType type);
    List<Invoice> findTop10ByBusinessIdOrderByCreatedAtDesc(UUID businessId);

    @Query("select coalesce(sum(i.totalAmount), 0) from Invoice i where i.business.id = :businessId and i.type = :type and i.invoiceDate between :from and :to and i.status <> 'CANCELLED'")
    BigDecimal sumTotal(UUID businessId, InvoiceType type, LocalDate from, LocalDate to);

    @Query("select coalesce(sum(i.totalGst), 0) from Invoice i where i.business.id = :businessId and i.type = :type and i.invoiceDate between :from and :to and i.status <> 'CANCELLED'")
    BigDecimal sumGst(UUID businessId, InvoiceType type, LocalDate from, LocalDate to);

    @Query("select coalesce(sum(i.totalAmount - i.paidAmount), 0) from Invoice i where i.business.id = :businessId and i.dueDate < :today and i.status in ('SENT', 'PARTIALLY_PAID', 'OVERDUE')")
    BigDecimal overdueAmount(UUID businessId, LocalDate today);

    @Query("select i from Invoice i where i.business.id = :businessId and i.invoiceDate between :from and :to order by i.invoiceDate desc")
    List<Invoice> findForPeriod(UUID businessId, LocalDate from, LocalDate to);
}
