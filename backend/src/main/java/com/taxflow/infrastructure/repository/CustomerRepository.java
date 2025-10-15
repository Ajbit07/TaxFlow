package com.taxflow.infrastructure.repository;

import com.taxflow.domain.model.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID>, JpaSpecificationExecutor<Customer> {
    Page<Customer> findByBusinessId(UUID businessId, Pageable pageable);
    List<Customer> findTop10ByBusinessIdAndNameContainingIgnoreCaseOrderByName(UUID businessId, String name);
    boolean existsByBusinessIdAndGstin(UUID businessId, String gstin);

    @Query("select coalesce(sum(c.outstandingBalance), 0) from Customer c where c.business.id = :businessId")
    BigDecimal totalOutstanding(UUID businessId);
}
