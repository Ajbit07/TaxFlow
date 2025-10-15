package com.taxflow.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.taxflow.application.service.BackupService;
import com.taxflow.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class BackupController {
    private final BackupService backupService;

    @GetMapping("/api/businesses/{businessId}/backup/export")
    public ApiResponse<Map<String, Object>> export(@PathVariable UUID businessId) {
        return ApiResponse.ok(backupService.export(businessId));
    }

    @PostMapping("/api/backup/import")
    public ApiResponse<Map<String, Integer>> importData(@RequestParam UUID businessId, @RequestBody JsonNode payload) {
        return ApiResponse.message("Backup imported", backupService.importData(businessId, payload));
    }
}
