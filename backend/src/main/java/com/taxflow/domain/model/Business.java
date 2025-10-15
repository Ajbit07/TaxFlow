package com.taxflow.domain.model;

import com.taxflow.domain.enums.BusinessType;
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

@Entity
@Table(name = "businesses")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Business extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User owner;

    @Column(length = 15)
    private String gstin;

    @Column(nullable = false, length = 10)
    private String pan;

    @Column(nullable = false)
    private String businessName;

    @Column(nullable = false)
    private String ownerName;

    @Column(nullable = false, length = 80)
    private String state;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private BusinessType businessType;

    @Column(nullable = false, length = 9)
    private String financialYear;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, length = 12)
    private String language;

    @Column(nullable = false)
    private boolean darkMode;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(nullable = false)
    private String email;
}
