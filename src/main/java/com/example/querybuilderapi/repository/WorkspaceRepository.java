package com.example.querybuilderapi.repository;

import com.example.querybuilderapi.model.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for the {@link Workspace} entity.
 */
@Repository
public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {

    /** Find workspace by its URL-safe slug (e.g., "acme-corp"). */
    Optional<Workspace> findBySlug(String slug);

    /** Check if a workspace with the given slug already exists. */
    boolean existsBySlug(String slug);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE Workspace w SET w.createdBy = null WHERE w.createdBy.id = :accountId")
    void nullCreatedByByAccountId(@org.springframework.data.repository.query.Param("accountId") Long accountId);
}
