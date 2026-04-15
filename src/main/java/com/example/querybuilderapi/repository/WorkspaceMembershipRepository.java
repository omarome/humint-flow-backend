package com.example.querybuilderapi.repository;

import com.example.querybuilderapi.model.WorkspaceMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for the {@link WorkspaceMembership} join entity.
 */
@Repository
public interface WorkspaceMembershipRepository extends JpaRepository<WorkspaceMembership, Long> {

    /**
     * Find a specific user's membership in a specific workspace.
     * Used by {@link com.example.querybuilderapi.security.WorkspaceResolutionFilter}.
     */
    Optional<WorkspaceMembership> findByWorkspaceIdAndAccountId(Long workspaceId, Long accountId);

    /**
     * Find all workspaces a user belongs to (for the workspace switcher).
     */
    @Query("SELECT m FROM WorkspaceMembership m JOIN FETCH m.workspace WHERE m.account.id = :accountId ORDER BY m.joinedAt ASC")
    List<WorkspaceMembership> findAllByAccountIdWithWorkspace(@Param("accountId") Long accountId);

    /**
     * Find all members of a specific workspace (for the admin member list).
     */
    @Query("SELECT m FROM WorkspaceMembership m JOIN FETCH m.account WHERE m.workspace.id = :workspaceId ORDER BY m.joinedAt ASC")
    List<WorkspaceMembership> findAllByWorkspaceIdWithAccount(@Param("workspaceId") Long workspaceId);

    /** Check membership existence (fast path, no entity load). */
    boolean existsByWorkspaceIdAndAccountId(Long workspaceId, Long accountId);

    /**
     * Count WORKSPACE_OWNER members in a workspace.
     * Used to guard against removing the last owner.
     */
    @Query("SELECT COUNT(m) FROM WorkspaceMembership m WHERE m.workspace.id = :workspaceId AND m.role = 'WORKSPACE_OWNER'")
    long countOwnersByWorkspaceId(@Param("workspaceId") Long workspaceId);

    /**
     * Finds the oldest membership for a user — used as the default workspace
     * when no X-Workspace-Id header is provided.
     */
    @Query("SELECT m FROM WorkspaceMembership m JOIN FETCH m.workspace WHERE m.account.id = :accountId ORDER BY m.joinedAt ASC")
    List<WorkspaceMembership> findOldestMembershipByAccountId(@Param("accountId") Long accountId);
}
