package com.taxflow.infrastructure.repository;

import com.taxflow.domain.enums.RuleType;
import com.taxflow.domain.model.BusinessRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface BusinessRuleRepository extends JpaRepository<BusinessRule, UUID> {
    @Query("select r from BusinessRule r where r.enabled = true and r.ruleType = :type and (r.business.id = :businessId or r.business is null)")
    List<BusinessRule> activeRules(UUID businessId, RuleType type);
}
