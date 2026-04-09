package com.example.querybuilderapi.controller;

import com.example.querybuilderapi.model.Attachment;
import com.example.querybuilderapi.model.AuthAccount;
import com.example.querybuilderapi.repository.AttachmentRepository;
import com.example.querybuilderapi.repository.AuthAccountRepository;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.firebase.cloud.StorageClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * File attachment API for any CRM entity.
 *
 * Files are uploaded to Firebase Storage and metadata is stored in PostgreSQL.
 *
 * GET    /api/attachments/{entityType}/{entityId}   — list attachments
 * POST   /api/attachments/{entityType}/{entityId}   — upload a file (multipart/form-data)
 * DELETE /api/attachments/{attachmentId}            — soft-delete + remove from Storage
 */
@RestController
@RequestMapping("/api/attachments")
public class AttachmentController {

    private static final Logger log = LoggerFactory.getLogger(AttachmentController.class);
    private static final long MAX_FILE_BYTES = 25 * 1024 * 1024L; // 25 MB

    @Value("${firebase.storage.bucket:#{null}}")
    private String storageBucket;

    private final AttachmentRepository attachmentRepository;
    private final AuthAccountRepository authAccountRepository;

    public AttachmentController(AttachmentRepository attachmentRepository,
                                AuthAccountRepository authAccountRepository) {
        this.attachmentRepository = attachmentRepository;
        this.authAccountRepository = authAccountRepository;
    }

    // ── List ──────────────────────────────────────────────────────────────

    @GetMapping("/{entityType}/{entityId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Map<String, Object>>> list(
            @PathVariable String entityType,
            @PathVariable UUID entityId) {

        List<Attachment> attachments = attachmentRepository
                .findByEntityTypeAndEntityIdAndIsDeletedFalseOrderByCreatedAtDesc(
                        entityType.toUpperCase(), entityId);

        List<Map<String, Object>> result = attachments.stream().map(this::toMap).toList();
        return ResponseEntity.ok(result);
    }

    // ── Upload ────────────────────────────────────────────────────────────

    @PostMapping("/{entityType}/{entityId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> upload(
            @PathVariable String entityType,
            @PathVariable UUID entityId,
            @RequestParam("file") MultipartFile file,
            Authentication auth) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        if (file.getSize() > MAX_FILE_BYTES) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "File exceeds 25 MB limit");
        }

        AuthAccount uploader = resolveUploader(auth.getName());
        UUID attachmentId = UUID.randomUUID();
        String safeFileName = sanitizeFileName(file.getOriginalFilename());
        String storagePath = String.format("attachments/%s/%s/%s/%s",
                entityType.toUpperCase(), entityId, attachmentId, safeFileName);

        String downloadUrl = null;
        if (storageBucket != null) {
            try {
                Storage storage = StorageClient.getInstance().bucket(storageBucket).getStorage();
                BlobId blobId = BlobId.of(storageBucket, storagePath);
                BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                        .setContentType(file.getContentType())
                        .build();
                storage.create(blobInfo, file.getBytes());

                // Generate a signed URL valid for 7 days
                URL signedUrl = storage.signUrl(blobInfo, 7, TimeUnit.DAYS,
                        Storage.SignUrlOption.withV4Signature());
                downloadUrl = signedUrl.toString();
            } catch (Exception e) {
                log.warn("Firebase Storage upload failed: {} — storing metadata without URL", e.getMessage());
            }
        }

        Attachment attachment = new Attachment();
        attachment.setEntityType(entityType.toUpperCase());
        attachment.setEntityId(entityId);
        attachment.setUploader(uploader);
        attachment.setFileName(safeFileName);
        attachment.setContentType(file.getContentType());
        attachment.setFileSize(file.getSize());
        attachment.setStoragePath(storagePath);
        attachment.setDownloadUrl(downloadUrl);

        Attachment saved = attachmentRepository.save(attachment);
        return ResponseEntity.status(HttpStatus.CREATED).body(toMap(saved));
    }

    // ── Delete ────────────────────────────────────────────────────────────

    @DeleteMapping("/{attachmentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> delete(
            @PathVariable UUID attachmentId,
            Authentication auth) {

        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        AuthAccount caller = resolveUploader(auth.getName());
        boolean isAdmin = caller.getRole() == AuthAccount.Role.ADMIN;
        if (!isAdmin && !attachment.getUploader().getId().equals(caller.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Can only delete your own attachments");
        }

        // Remove from Firebase Storage
        if (storageBucket != null && attachment.getStoragePath() != null) {
            try {
                Storage storage = StorageClient.getInstance().bucket(storageBucket).getStorage();
                storage.delete(BlobId.of(storageBucket, attachment.getStoragePath()));
            } catch (Exception e) {
                log.warn("Could not delete storage object {}: {}", attachment.getStoragePath(), e.getMessage());
            }
        }

        attachment.setIsDeleted(true);
        attachmentRepository.save(attachment);
        return ResponseEntity.noContent().build();
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private AuthAccount resolveUploader(String firebaseUid) {
        return authAccountRepository.findByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unknown user"));
    }

    private String sanitizeFileName(String original) {
        if (original == null) return "file";
        return original.replaceAll("[^a-zA-Z0-9._\\-]", "_");
    }

    private Map<String, Object> toMap(Attachment a) {
        return Map.of(
                "id",           a.getId(),
                "fileName",     a.getFileName(),
                "contentType",  a.getContentType() != null ? a.getContentType() : "",
                "fileSize",     a.getFileSize() != null ? a.getFileSize() : 0L,
                "downloadUrl",  a.getDownloadUrl() != null ? a.getDownloadUrl() : "",
                "uploaderId",   a.getUploader().getId(),
                "uploaderName", a.getUploader().getDisplayName(),
                "createdAt",    a.getCreatedAt() != null ? a.getCreatedAt().toString() : ""
        );
    }
}
