package com.taxflow.domain.model;

import com.taxflow.domain.enums.FilingStatus;
import com.taxflow.domain.enums.FilingType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "tax_filings")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaxFiling extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Business business;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FilingType filingType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FilingStatus status;

    @Column(nullable = false)
    private LocalDate periodStart;

    @Column(nullable = false)
    private LocalDate periodEnd;

    @Column(nullable = false)
    private int progressPercent;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal taxDue;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal lateFee;

    @Column(columnDefinition = "TEXT")
    private String summaryJson;

    private OffsetDateTime submittedAt;
}
