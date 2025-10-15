package com.taxflow.infrastructure.repository;

import com.taxflow.domain.enums.DocumentType;
import com.taxflow.domain.model.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID> {
    Page<Document> findByBusinessId(UUID businessId, Pageable pageable);
    List<Document> findByBusinessIdAndDocumentType(UUID businessId, DocumentType documentType);
    int countByBusinessIdAndDocumentType(UUID businessId, DocumentType documentType);
}
