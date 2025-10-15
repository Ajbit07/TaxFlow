package com.taxflow.application.service;

import com.taxflow.application.dto.TaxFlowDtos.DocumentResponse;
import com.taxflow.common.InputSanitizer;
import com.taxflow.common.PageResponse;
import com.taxflow.common.exception.NotFoundException;
import com.taxflow.domain.enums.DocumentType;
import com.taxflow.domain.enums.TaskScope;
import com.taxflow.domain.model.Business;
import com.taxflow.domain.model.Document;
import com.taxflow.infrastructure.repository.DocumentRepository;
import com.taxflow.infrastructure.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentService {
    private final DocumentRepository documentRepository;
    private final BusinessService businessService;
    private final AccessService accessService;
    private final InputSanitizer sanitizer;
    private final AuditService auditService;
    private final FileStorageService fileStorageService;

    @Transactional(readOnly = true)
    public PageResponse<DocumentResponse> list(UUID businessId, Pageable pageable) {
        businessService.business(businessId, TaskScope.DOCUMENT);
        return PageResponse.from(documentRepository.findByBusinessId(businessId, pageable), this::toResponse);
    }

    @Transactional
    public DocumentResponse upload(UUID businessId, DocumentType documentType, MultipartFile file) {
        Business business = businessService.business(businessId, TaskScope.DOCUMENT);
        int version = documentRepository.countByBusinessIdAndDocumentType(businessId, documentType) + 1;
        String storagePath = fileStorageService.store(businessId, documentType.name().toLowerCase(), file);
        Document document = documentRepository.save(Document.builder()
                .business(business)
                .uploadedBy(accessService.currentUser())
                .documentType(documentType)
                .fileName(sanitizer.clean(file.getOriginalFilename() == null ? "file" : file.getOriginalFilename()))
                .contentType(sanitizer.clean(file.getContentType()))
                .fileSize(file.getSize())
                .version(version)
                .storagePath(storagePath)
                .status("STORED")
                .build());
        auditService.log(businessId, "UPLOAD", "DOCUMENT", document.getId(), null, document.getFileName());
        return toResponse(document);
    }

    @Transactional(readOnly = true)
    public StoredFile download(UUID businessId, UUID documentId) {
        businessService.business(businessId, TaskScope.DOCUMENT);
        Document document = require(businessId, documentId);
        return new StoredFile(document.getFileName(), document.getContentType(), fileStorageService.load(document.getStoragePath()));
    }

    public record StoredFile(String fileName, String contentType, byte[] bytes) {
    }

    @Transactional(readOnly = true)
    public DocumentResponse preview(UUID businessId, UUID documentId) {
        businessService.business(businessId, TaskScope.DOCUMENT);
        return toResponse(require(businessId, documentId));
    }

    @Transactional
    public void delete(UUID businessId, UUID documentId) {
        businessService.business(businessId, TaskScope.DOCUMENT);
        Document document = require(businessId, documentId);
        fileStorageService.delete(document.getStoragePath());
        documentRepository.delete(document);
        auditService.log(businessId, "DELETE", "DOCUMENT", documentId, document.getFileName(), null);
    }

    @Transactional(readOnly = true)
    public PageResponse<DocumentResponse> versionHistory(UUID businessId, DocumentType type, Pageable pageable) {
        businessService.business(businessId, TaskScope.DOCUMENT);
        return PageResponse.from(documentRepository.findByBusinessId(businessId, pageable), this::toResponse);
    }

    private Document require(UUID businessId, UUID documentId) {
        Document document = documentRepository.findById(documentId).orElseThrow(() -> new NotFoundException("Document not found"));
        if (!document.getBusiness().getId().equals(businessId)) {
            throw new NotFoundException("Document not found");
        }
        return document;
    }

    private DocumentResponse toResponse(Document document) {
        return new DocumentResponse(document.getId(), document.getDocumentType(), document.getFileName(), document.getContentType(),
                document.getStoragePath(), document.getFileSize(), document.getVersion(), document.getStatus(),
                document.getExtractedFieldsJson(), document.getCreatedAt());
    }
}
