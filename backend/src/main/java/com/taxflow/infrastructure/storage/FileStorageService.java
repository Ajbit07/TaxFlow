package com.taxflow.infrastructure.storage;

import com.taxflow.common.exception.BusinessException;
import com.taxflow.common.exception.NotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

/**
 * Stores uploaded files on the local filesystem under a configurable root directory.
 * Files are grouped by business and namespaced with a random prefix so that
 * uploads can never overwrite each other or escape the storage root.
 */
@Service
public class FileStorageService {
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf", "image/png", "image/jpeg", "image/webp",
            "text/csv", "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final Path root;

    public FileStorageService(@Value("${app.storage.path}") String storagePath) {
        this.root = Path.of(storagePath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to create storage directory " + root, ex);
        }
    }

    public String store(UUID businessId, String folder, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Uploaded file is empty");
        }
        if (file.getContentType() == null || !ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new BusinessException("Unsupported file type: " + file.getContentType());
        }
        String original = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String safeName = original.replaceAll("[^A-Za-z0-9._-]", "_");
        String relative = businessId + "/" + folder + "/" + UUID.randomUUID() + "-" + safeName;
        Path target = root.resolve(relative).normalize();
        if (!target.startsWith(root)) {
            throw new BusinessException("Invalid storage path");
        }
        try {
            Files.createDirectories(target.getParent());
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to store file", ex);
        }
        return relative;
    }

    public byte[] load(String relativePath) {
        Path target = root.resolve(relativePath).normalize();
        if (!target.startsWith(root) || !Files.exists(target)) {
            throw new NotFoundException("Stored file not found");
        }
        try {
            return Files.readAllBytes(target);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to read stored file", ex);
        }
    }

    public void delete(String relativePath) {
        Path target = root.resolve(relativePath).normalize();
        if (!target.startsWith(root)) {
            return;
        }
        try {
            Files.deleteIfExists(target);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to delete stored file", ex);
        }
    }
}
