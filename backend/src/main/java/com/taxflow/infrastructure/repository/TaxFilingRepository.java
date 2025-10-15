package com.taxflow.infrastructure.repository;

import com.taxflow.domain.enums.FilingStatus;
import com.taxflow.domain.enums.FilingType;
import com.taxflow.domain.model.TaxFiling;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaxFilingRepository extends JpaRepository<TaxFiling, UUID> {
    Page<TaxFiling> findByBusinessId(UUID businessId, Pageable pageable);
    List<TaxFiling> findByBusinessIdAndStatusNot(UUID businessId, FilingStatus status);
    Optional<TaxFiling> findByBusinessIdAndFilingTypeAndPeriodStartAndPeriodEnd(UUID businessId, FilingType type, LocalDate start, LocalDate end);
    long countByBusinessIdAndStatusNot(UUID businessId, FilingStatus status);
}
