package com.taxflow.infrastructure.repository;

import com.taxflow.domain.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    Page<Notification> findByBusinessIdAndUserId(UUID businessId, UUID userId, Pageable pageable);
    List<Notification> findTop10ByBusinessIdAndUserIdOrderByCreatedAtDesc(UUID businessId, UUID userId);
    long countByBusinessIdAndUserIdAndReadFlagFalse(UUID businessId, UUID userId);
}
