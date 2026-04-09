package com.example.querybuilderapi.repository;

import com.example.querybuilderapi.model.Opportunity;
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
 * Spring Data JPA repository for the Opportunity entity.
 * Uses @EntityGraph to eagerly load Organization and Contact relationships
 * in a single query, preventing N+1 problems.
 */
@Repository
public interface OpportunityRepository extends JpaRepository<Opportunity, UUID>, JpaSpecificationExecutor<Opportunity> {

    /**
     * Returns all non-deleted opportunities with Organization + Contact loaded.
     */
    @EntityGraph(attributePaths = {"organization", "primaryContact", "assignedTo"})
    Page<Opportunity> findAllByIsDeletedFalse(Pageable pageable);

    /**
     * Returns all non-deleted opportunities without pagination.
     */
    @EntityGraph(attributePaths = {"organization", "primaryContact", "assignedTo"})
    List<Opportunity> findAllByIsDeletedFalse();

    /**
     * Finds a single non-deleted opportunity by ID with relationships loaded.
     */
    @EntityGraph(attributePaths = {"organization", "primaryContact", "assignedTo"})
    Optional<Opportunity> findByIdAndIsDeletedFalse(UUID id);

    /**
     * Finds all non-deleted opportunities for a given organization.
     */
    @EntityGraph(attributePaths = {"organization", "primaryContact", "assignedTo"})
    List<Opportunity> findAllByOrganizationIdAndIsDeletedFalse(UUID organizationId);

    /**
     * Finds all non-deleted opportunities linked to a specific contact.
     */
    @EntityGraph(attributePaths = {"organization", "primaryContact", "assignedTo"})
    List<Opportunity> findAllByPrimaryContactIdAndIsDeletedFalse(UUID contactId);

    /**
     * Finds all non-deleted opportunities in a given deal stage.
     * Used for the Kanban pipeline view.
     */
    @EntityGraph(attributePaths = {"organization", "primaryContact", "assignedTo"})
    List<Opportunity> findAllByStageAndIsDeletedFalse(String stage);

    /**
     * Search opportunities by name.
     */
    @EntityGraph(attributePaths = {"organization", "primaryContact", "assignedTo"})
    @Query("SELECT o FROM Opportunity o WHERE o.isDeleted = false AND LOWER(o.name) LIKE LOWER(CONCAT('%', :q, '%'))")
    Page<Opportunity> searchByName(@Param("q") String query, Pageable pageable);

    /**
     * Count non-deleted opportunities.
     */
    long countByIsDeletedFalse();

    /**
     * Count open deals assigned to a specific team member — used for team dashboard stats.
     */
    long countByAssignedToIdAndIsDeletedFalse(Long assignedToId);
}
