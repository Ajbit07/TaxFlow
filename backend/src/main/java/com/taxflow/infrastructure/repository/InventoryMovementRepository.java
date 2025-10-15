package com.taxflow.infrastructure.repository;

import com.taxflow.domain.model.InventoryMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, UUID> {
    Page<InventoryMovement> findByBusinessId(UUID businessId, Pageable pageable);
    List<InventoryMovement> findTop20ByBusinessIdOrderByCreatedAtDesc(UUID businessId);
}
