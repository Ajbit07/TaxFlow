package com.taxflow.infrastructure.repository;

import com.taxflow.domain.model.Business;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface BusinessRepository extends JpaRepository<Business, UUID> {
    List<Business> findByOwnerId(UUID ownerId);
    boolean existsByIdAndOwnerId(UUID businessId, UUID ownerId);

    @Query("""
            select distinct b from Business b
            where b.owner.id = :userId
               or exists (select m.id from BusinessMember m where m.business = b and m.user.id = :userId)
            order by b.businessName
            """)
    List<Business> findAccessible(UUID userId);
}
