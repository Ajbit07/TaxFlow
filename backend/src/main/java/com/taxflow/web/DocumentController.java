package com.taxflow.web;

import com.taxflow.application.dto.TaxFlowDtos.DocumentResponse;
import com.taxflow.application.service.DocumentService;
import com.taxflow.common.ApiResponse;
import com.taxflow.common.PageResponse;
import com.taxflow.domain.enums.DocumentType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/businesses/{businessId}/documents")
@RequiredArgsConstructor
public class DocumentController {
    private final DocumentService documentService;

    @GetMapping
    public ApiResponse<PageResponse<DocumentResponse>> list(@PathVariable UUID businessId,
                                                            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.ok(documentService.list(businessId, pageable));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<DocumentResponse> upload(@PathVariable UUID businessId,
                                                @RequestParam DocumentType documentType,
                                                @RequestParam("file") MultipartFile file) {
        return ApiResponse.message("Document uploaded", documentService.upload(businessId, documentType, file));
    }

    @GetMapping("/{documentId}")
    public ApiResponse<DocumentResponse> preview(@PathVariable UUID businessId, @PathVariable UUID documentId) {
        return ApiResponse.ok(documentService.preview(businessId, documentId));
    }

    @GetMapping("/{documentId}/download")
    public ResponseEntity<byte[]> download(@PathVariable UUID businessId, @PathVariable UUID documentId) {
        DocumentService.StoredFile file = documentService.download(businessId, documentId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.fileName() + "\"")
                .contentType(MediaType.parseMediaType(file.contentType()))
                .body(file.bytes());
    }

    @GetMapping("/versions")
    public ApiResponse<PageResponse<DocumentResponse>> versions(@PathVariable UUID businessId,
                                                                @RequestParam DocumentType documentType,
                                                                @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.ok(documentService.versionHistory(businessId, documentType, pageable));
    }

    @DeleteMapping("/{documentId}")
    public ApiResponse<Void> delete(@PathVariable UUID businessId, @PathVariable UUID documentId) {
        documentService.delete(businessId, documentId);
        return ApiResponse.message("Document deleted", null);
    }
}
