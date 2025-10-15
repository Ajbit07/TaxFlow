package com.taxflow.application.service;

import com.taxflow.application.dto.TaxFlowDtos.AuditResponse;
import com.taxflow.common.PageResponse;
import com.taxflow.domain.model.AuditLog;
import com.taxflow.infrastructure.repository.AuditLogRepository;
import com.taxflow.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditService {
    private final AuditLogRepository auditLogRepository;

    public void log(UUID businessId, String action, String entityType, Object entityId, String oldValue, String newValue) {
        UUID userId;
        try {
            userId = SecurityUtils.userId();
        } catch (RuntimeException ex) {
            userId = null;
        }
        auditLogRepository.save(AuditLog.builder()
                .userId(userId)
                .businessId(businessId)
                .actionTime(OffsetDateTime.now())
                .ipAddress("127.0.0.1")
                .action(action)
                .entityType(entityType)
                .entityId(entityId == null ? null : entityId.toString())
                .oldValue(oldValue)
                .newValue(newValue)
                .success(true)
                .build());
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditResponse> list(UUID businessId, Pageable pageable) {
        return PageResponse.from(auditLogRepository.findByBusinessId(businessId, pageable), log ->
                new AuditResponse(log.getId(), log.getUserId(), log.getBusinessId(), log.getActionTime(), log.getIpAddress(),
                        log.getAction(), log.getEntityType(), log.getEntityId(), log.getOldValue(), log.getNewValue(),
                        log.isSuccess(), log.getErrorMessage()));
    }

    public void failure(UUID businessId, String action, String entityType, String errorMessage) {
        auditLogRepository.save(AuditLog.builder()
                .userId(null)
                .businessId(businessId)
                .actionTime(OffsetDateTime.now())
                .ipAddress("127.0.0.1")
                .action(action)
                .entityType(entityType)
                .success(false)
                .errorMessage(errorMessage)
                .build());
    }
}
