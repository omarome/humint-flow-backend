package com.example.querybuilderapi.security;

import com.example.querybuilderapi.model.AuthAccount;
import com.example.querybuilderapi.model.WorkspaceMembership;
import com.example.querybuilderapi.repository.AuthAccountRepository;
import com.example.querybuilderapi.repository.WorkspaceMembershipRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Resolves the workspace context for every authenticated request.
 *
 * Must run AFTER FirebaseTokenFilter/JwtAuthenticationFilter in the filter chain.
 *
 * Algorithm:
 *  1. If unauthenticated → skip (Spring Security will reject it downstream).
 *  2. If the caller is SUPER_ADMIN → set bypass flag and skip membership check.
 *  3. Read "X-Workspace-Id" header.
 *     - If present → validate membership in that workspace.
 *     - If absent  → fall back to the caller's oldest (default) workspace.
 *  4. If membership found → populate {@link WorkspaceContext}.
 *  5. If membership NOT found → 403 Forbidden.
 *
 * Public / non-CRM endpoints (/api/auth/**, /api/workspaces, /actuator/**)
 * bypass workspace validation via the path exclusion list.
 */
@Component
public class WorkspaceResolutionFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(WorkspaceResolutionFilter.class);
    private static final String WORKSPACE_HEADER = "X-Workspace-Id";

    private final AuthAccountRepository        accountRepository;
    private final WorkspaceMembershipRepository membershipRepository;
    private final WorkspaceContext             workspaceContext;

    public WorkspaceResolutionFilter(AuthAccountRepository accountRepository,
                                     WorkspaceMembershipRepository membershipRepository,
                                     WorkspaceContext workspaceContext) {
        this.accountRepository    = accountRepository;
        this.membershipRepository = membershipRepository;
        this.workspaceContext     = workspaceContext;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Bypass workspace resolution for auth, health, and workspace-creation
        // endpoints that must work before a workspace is known.
        return path.startsWith("/api/auth/")
            || path.startsWith("/actuator/")
            || path.startsWith("/login")
            || path.startsWith("/oauth2/")
            || path.equals("/api/me/active-workspace")
            || (path.equals("/api/workspaces") && "POST".equals(request.getMethod()))
            || (path.equals("/api/workspaces") && "GET".equals(request.getMethod()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            chain.doFilter(request, response);
            return;
        }

        // Resolve the caller's AuthAccount from the principal name (email)
        String email = auth.getName();
        AuthAccount account = accountRepository.findByEmail(email).orElse(null);
        if (account == null) {
            chain.doFilter(request, response);
            return;
        }

        // SUPER_ADMIN bypass — can access any workspace without membership
        if (account.getRole() == AuthAccount.Role.SUPER_ADMIN) {
            workspaceContext.setSuperAdminBypass(true);
            workspaceContext.setEffectiveRole(AuthAccount.Role.SUPER_ADMIN);

            String headerWsId = request.getHeader(WORKSPACE_HEADER);
            if (headerWsId != null) {
                try {
                    workspaceContext.setWorkspaceId(Long.parseLong(headerWsId));
                } catch (NumberFormatException ignored) {}
            }
            chain.doFilter(request, response);
            return;
        }

        // Resolve workspace ID from header or default
        Long workspaceId = resolveWorkspaceId(request, account.getId());
        if (workspaceId == null) {
            // User has no workspaces at all
            reject(response, "No workspace membership found. Contact your administrator.");
            return;
        }

        // Validate membership
        WorkspaceMembership membership = membershipRepository
                .findByWorkspaceIdAndAccountId(workspaceId, account.getId())
                .orElse(null);

        if (membership == null) {
            log.warn("Account {} attempted to access workspace {} without membership",
                     account.getEmail(), workspaceId);
            reject(response, "Not a member of workspace " + workspaceId);
            return;
        }

        workspaceContext.setWorkspaceId(workspaceId);
        workspaceContext.setEffectiveRole(membership.getRole());

        log.debug("Workspace context resolved: workspaceId={}, role={} for {}",
                  workspaceId, membership.getRole(), email);

        chain.doFilter(request, response);
    }

    // ── Private helpers ───────────────────────────────────────────────────

    private Long resolveWorkspaceId(HttpServletRequest request, Long accountId) {
        String header = request.getHeader(WORKSPACE_HEADER);
        if (header != null && !header.isBlank()) {
            try {
                return Long.parseLong(header.trim());
            } catch (NumberFormatException e) {
                log.warn("Invalid X-Workspace-Id header value: '{}'", header);
                return null;
            }
        }

        // Fall back to the user's oldest (default) workspace
        List<WorkspaceMembership> memberships =
                membershipRepository.findOldestMembershipByAccountId(accountId);
        return memberships.isEmpty() ? null : memberships.get(0).getWorkspace().getId();
    }

    private void reject(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"error\":\"" + message + "\"}");
    }
}
