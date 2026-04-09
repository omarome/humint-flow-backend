package com.example.querybuilderapi.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * A file attachment linked to any CRM entity.
 *
 * Files are stored in Firebase Storage under the path:
 *   attachments/{entityType}/{entityId}/{attachmentId}/{originalFileName}
 *
 * The metadata (name, size, type, URL) is persisted in PostgreSQL for
 * fast listing and search without hitting Firebase Storage on every request.
 *
 * Maps to the "attachments" table.
 */
@Entity
@Table(name = "attachments", indexes = {
    @Index(name = "idx_attachment_entity", columnList = "entity_type, entity_id"),
    @Index(name = "idx_attachment_uploader", columnList = "uploader_id")
})
public class Attachment extends BaseEntity {

    @Column(name = "entity_type", nullable = false, length = 30)
    private String entityType;

    @Column(name = "entity_id", nullable = false, columnDefinition = "uuid")
    private UUID entityId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploader_id", nullable = false)
    private AuthAccount uploader;

    @Column(name = "file_name", nullable = false)
    @NotBlank
    @Size(max = 500)
    private String fileName;

    @Column(name = "content_type", length = 100)
    private String contentType;

    /** File size in bytes. */
    @Column(name = "file_size")
    private Long fileSize;

    /**
     * The Firebase Storage path — used to generate signed download URLs.
     * Format: attachments/{entityType}/{entityId}/{attachmentId}/{fileName}
     */
    @Column(name = "storage_path", columnDefinition = "TEXT", nullable = false)
    private String storagePath;

    /**
     * Signed download URL. Rotated periodically; clients should re-fetch
     * if the URL expires (Firebase signed URLs default to 7 days).
     */
    @Column(name = "download_url", columnDefinition = "TEXT")
    private String downloadUrl;

    public Attachment() {}

    // ─── Getters & Setters ────────────────────────────────────────────────

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }

    public UUID getEntityId() { return entityId; }
    public void setEntityId(UUID entityId) { this.entityId = entityId; }

    public AuthAccount getUploader() { return uploader; }
    public void setUploader(AuthAccount uploader) { this.uploader = uploader; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public String getStoragePath() { return storagePath; }
    public void setStoragePath(String storagePath) { this.storagePath = storagePath; }

    public String getDownloadUrl() { return downloadUrl; }
    public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }
}
