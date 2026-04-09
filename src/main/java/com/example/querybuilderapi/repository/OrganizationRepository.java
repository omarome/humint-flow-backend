package com.example.querybuilderapi.repository;

import com.example.querybuilderapi.model.Organization;
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
 * Spring Data JPA repository for the Organization entity.
 * All default queries filter out soft-deleted records.
 */
@Repository
public interface OrganizationRepository extends JpaRepository<Organization, UUID>, JpaSpecificationExecutor<Organization> {

    /**
     * Returns all non-deleted organizations.
     */
    @EntityGraph(attributePaths = {"assignedTo"})
    List<Organization> findAllByIsDeletedFalse();

    /**
     * Returns all non-deleted organizations, paginated.
     */
    @EntityGraph(attributePaths = {"assignedTo"})
    Page<Organization> findAllByIsDeletedFalse(Pageable pageable);

    /**
     * Finds a single non-deleted organization by ID.
     */
    @EntityGraph(attributePaths = {"assignedTo"})
    Optional<Organization> findByIdAndIsDeletedFalse(UUID id);

    /**
     * Search organizations by name (case-insensitive, partial match).
     * Only returns non-deleted records.
     */
    @EntityGraph(attributePaths = {"assignedTo"})
    @Query("SELECT o FROM Organization o WHERE o.isDeleted = false AND LOWER(o.name) LIKE LOWER(CONCAT('%', :q, '%'))")
    Page<Organization> searchByName(@Param("q") String query, Pageable pageable);

    /**
     * Count non-deleted organizations.
     */
    long countByIsDeletedFalse();
}
