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
}
