package com.example.querybuilderapi.repository;

import com.example.querybuilderapi.model.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, UUID> {

    List<Attachment> findByEntityTypeAndEntityIdAndIsDeletedFalseOrderByCreatedAtDesc(
            String entityType, UUID entityId);

    long countByEntityTypeAndEntityIdAndIsDeletedFalse(String entityType, UUID entityId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE Attachment a SET a.uploader = null WHERE a.uploader.id = :accountId")
    void nullUploaderByAccountId(@org.springframework.data.repository.query.Param("accountId") Long accountId);
}
