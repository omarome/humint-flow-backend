package com.example.querybuilderapi.repository;

import com.example.querybuilderapi.model.Activity;
import com.example.querybuilderapi.model.EntityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Activity CRUD with soft delete filtering.
 */
@Repository
public interface ActivityRepository extends JpaRepository<Activity, UUID>, JpaSpecificationExecutor<Activity> {

    /**
     * Fetches a paginated timeline of non-deleted activities for a given entity.
     */
    Page<Activity> findByEntityTypeAndEntityIdAndIsDeletedFalseOrderByCreatedAtDesc(
            EntityType entityType, UUID entityId, Pageable pageable);

    /**
     * Finds all non-deleted activities for a given entity.
     */
    java.util.List<Activity> findByEntityTypeAndEntityId(EntityType entityType, UUID entityId);

    /**
     * Finds a single non-deleted activity by ID.
     */
    Optional<Activity> findByIdAndIsDeletedFalse(UUID id);

    /**
     * Count total activities created by a given user — used for team dashboard stats.
     */
    long countByCreatedBy(Long createdBy);

    /**
     * Finds all non-deleted activities globally (useful for scheduled jobs).
     */
    java.util.List<Activity> findAllByIsDeletedFalse();

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE Activity a SET a.assignedTo = null WHERE a.assignedTo.id = :accountId")
    void nullAssignedToByAccountId(@org.springframework.data.repository.query.Param("accountId") Long accountId);

    /**
     * Paginated version — used by CalendarView and any global activity feed.
     */
    Page<Activity> findAllByIsDeletedFalse(Pageable pageable);
}
