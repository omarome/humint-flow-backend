package com.example.querybuilderapi.repository;

import com.example.querybuilderapi.model.Contact;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for the Contact entity.
 * <p>
 * Uses {@code @EntityGraph} to eagerly fetch the Organization relationship
 * in a single query, preventing the N+1 problem when listing contacts.
 */
@Repository
public interface ContactRepository extends JpaRepository<Contact, UUID>, JpaSpecificationExecutor<Contact> {

    /**
     * Returns all non-deleted contacts with their Organization eagerly loaded.
     * Solves N+1: one SQL join instead of N individual org lookups.
     */
    @EntityGraph(attributePaths = {"organization", "assignedTo"})
    Page<Contact> findAllByIsDeletedFalse(Pageable pageable);

    /**
     * Finds a single non-deleted contact by ID with Organization loaded.
     */
    @EntityGraph(attributePaths = {"organization", "assignedTo"})
    Optional<Contact> findByIdAndIsDeletedFalse(UUID id);

    /**
     * Finds all non-deleted contacts for a given organization.
     */
    @EntityGraph(attributePaths = {"organization", "assignedTo"})
    List<Contact> findAllByOrganizationIdAndIsDeletedFalse(UUID organizationId);

    /**
     * Search contacts by name (first or last, case-insensitive, partial match).
     * Only returns non-deleted records.
     */
    @EntityGraph(attributePaths = {"organization", "assignedTo"})
    @Query("SELECT c FROM Contact c WHERE c.isDeleted = false AND " +
           "(LOWER(c.firstName) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           " LOWER(c.lastName) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           " LOWER(c.email) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<Contact> searchByNameOrEmail(@Param("q") String query, Pageable pageable);

    /**
     * Finds contacts by lifecycle stage (for segmentation/filter engine).
     */
    @EntityGraph(attributePaths = {"organization", "assignedTo"})
    Page<Contact> findAllByLifecycleStageAndIsDeletedFalse(String lifecycleStage, Pageable pageable);

    /**
     * Count non-deleted contacts.
     */
    long countByIsDeletedFalse();
}
