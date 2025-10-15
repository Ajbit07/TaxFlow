package com.taxflow.application.service;

import com.taxflow.application.dto.TaxFlowDtos.NotificationResponse;
import com.taxflow.common.PageResponse;
import com.taxflow.common.exception.NotFoundException;
import com.taxflow.domain.enums.TaskScope;
import com.taxflow.domain.model.Notification;
import com.taxflow.infrastructure.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final BusinessService businessService;
    private final AccessService accessService;

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> list(UUID businessId, Pageable pageable) {
        businessService.business(businessId, TaskScope.NOTIFICATION);
        return PageResponse.from(notificationRepository.findByBusinessIdAndUserId(businessId, accessService.currentUser().getId(), pageable), this::toResponse);
    }

    @Transactional(readOnly = true)
    public long unreadCount(UUID businessId) {
        businessService.business(businessId, TaskScope.NOTIFICATION);
        return notificationRepository.countByBusinessIdAndUserIdAndReadFlagFalse(businessId, accessService.currentUser().getId());
    }

    @Transactional
    public NotificationResponse markRead(UUID businessId, UUID notificationId) {
        businessService.business(businessId, TaskScope.NOTIFICATION);
        Notification notification = notificationRepository.findById(notificationId).orElseThrow(() -> new NotFoundException("Notification not found"));
        if (!notification.getBusiness().getId().equals(businessId)) {
            throw new NotFoundException("Notification not found");
        }
        notification.setReadFlag(true);
        return toResponse(notification);
    }

    public NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(notification.getId(), notification.getType(), notification.getTitle(),
                notification.getMessage(), notification.isReadFlag(), notification.getActionUrl(),
                notification.getDueDate(), notification.getCreatedAt());
    }
}
