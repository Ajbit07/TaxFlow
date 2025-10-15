package com.taxflow.web;

import com.taxflow.application.dto.TaxFlowDtos.NotificationResponse;
import com.taxflow.application.service.NotificationService;
import com.taxflow.common.ApiResponse;
import com.taxflow.common.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/businesses/{businessId}/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;

    @GetMapping
    public ApiResponse<PageResponse<NotificationResponse>> list(@PathVariable UUID businessId,
                                                                @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.ok(notificationService.list(businessId, pageable));
    }

    @GetMapping("/unread-count")
    public ApiResponse<Map<String, Long>> unreadCount(@PathVariable UUID businessId) {
        return ApiResponse.ok(Map.of("unread", notificationService.unreadCount(businessId)));
    }

    @PutMapping("/{notificationId}/read")
    public ApiResponse<NotificationResponse> markRead(@PathVariable UUID businessId, @PathVariable UUID notificationId) {
        return ApiResponse.ok(notificationService.markRead(businessId, notificationId));
    }
}
