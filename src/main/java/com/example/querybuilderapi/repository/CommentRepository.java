package com.example.querybuilderapi.repository;

import com.example.querybuilderapi.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CommentRepository extends JpaRepository<Comment, UUID> {

    /** All top-level comments for an entity, oldest first. */
    List<Comment> findByEntityTypeAndEntityIdAndParentIdIsNullAndIsDeletedFalseOrderByCreatedAtAsc(
            String entityType, UUID entityId);

    /** Replies to a specific comment. */
    List<Comment> findByParentIdAndIsDeletedFalseOrderByCreatedAtAsc(UUID parentId);

    /** Count all non-deleted comments for an entity (used for badges). */
    long countByEntityTypeAndEntityIdAndIsDeletedFalse(String entityType, UUID entityId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE Comment c SET c.author = null WHERE c.author.id = :accountId")
    void nullAuthorByAccountId(@org.springframework.data.repository.query.Param("accountId") Long accountId);
}
