package com.taxflow.domain.model;

import com.taxflow.domain.enums.RuleSeverity;
import com.taxflow.domain.enums.RuleType;
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
@Table(name = "business_rules")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessRule extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    private Business business;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RuleType ruleType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RuleSeverity severity;

    @Column(nullable = false)
    private boolean enabled;

    @Column(nullable = false)
    private String expressionKey;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;
}
