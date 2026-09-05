package com.example.querybuilderapi.service;

import com.example.querybuilderapi.model.AuthAccount;
import com.example.querybuilderapi.model.Workspace;
import com.example.querybuilderapi.repository.AuthAccountRepository;
import com.example.querybuilderapi.repository.WorkspaceRepository;
import com.example.querybuilderapi.security.WorkspaceContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Utility service to extract the current request's authenticated user and
 * workspace from the security / workspace context.
 *
 * Used to auto-populate createdBy / updatedBy audit fields and the mandatory
 * {@code workspace_id} on CRM entities.
 */
@Service
public class AuditAwareService {

    private final AuthAccountRepository authAccountRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceContext workspaceContext;

    public AuditAwareService(AuthAccountRepository authAccountRepository,
                             WorkspaceRepository workspaceRepository,
                             WorkspaceContext workspaceContext) {
        this.authAccountRepository = authAccountRepository;
        this.workspaceRepository = workspaceRepository;
        this.workspaceContext = workspaceContext;
    }

    /**
     * Returns the ID of the currently authenticated AuthAccount,
     * or null if there is no authenticated user (e.g., during data seeding).
     */
    public Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        String principal = authentication.getName();
        if (principal == null || "anonymousUser".equals(principal)) {
            return null;
        }

        // The principal name is the account email (AuthAccount implements Principal).
        Optional<AuthAccount> account = authAccountRepository.findByEmail(principal);
        return account.map(AuthAccount::getId).orElse(null);
    }

    /**
     * The {@link Workspace} that records created during the current request must
     * be attached to (workspace_id is NOT NULL on the CRM tables).
     *
     * Uses the request's resolved workspace ({@link WorkspaceContext}, populated
     * by {@code WorkspaceResolutionFilter}); falls back to the {@code default}
     * workspace when the request carries none — e.g. a SUPER_ADMIN with no
     * {@code X-Workspace-Id} header, or a single-workspace deployment.
     *
     * @throws IllegalStateException if neither a resolved nor a default workspace exists
     */
    public Workspace getCurrentWorkspace() {
        Long id = workspaceContext.getWorkspaceId();
        if (id != null) {
            return workspaceRepository.findById(id).orElseThrow(() ->
                    new IllegalStateException("Resolved workspace " + id + " does not exist"));
        }
        return workspaceRepository.findBySlug("default").orElseThrow(() ->
                new IllegalStateException("No workspace on the request and no 'default' workspace exists"));
    }
}
