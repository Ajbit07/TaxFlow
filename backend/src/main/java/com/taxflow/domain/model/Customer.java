package com.taxflow.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "customers")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Customer extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Business business;

    @Column(nullable = false)
    private String name;

    @Column(length = 15)
    private String gstin;

    @Column(length = 10)
    private String pan;

    private String phone;
    private String email;
    private String address;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal outstandingBalance;
}
