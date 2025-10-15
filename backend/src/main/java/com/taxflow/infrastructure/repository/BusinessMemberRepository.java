package com.taxflow.infrastructure.repository;

import com.taxflow.domain.model.BusinessMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BusinessMemberRepository extends JpaRepository<BusinessMember, UUID> {
    Optional<BusinessMember> findByBusinessIdAndUserId(UUID businessId, UUID userId);
    boolean existsByBusinessIdAndUserId(UUID businessId, UUID userId);
}
