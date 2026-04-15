package com.example.querybuilderapi.security;

import com.example.querybuilderapi.exception.AccountNotInvitedException;
import com.example.querybuilderapi.model.AuthAccount;
import com.example.querybuilderapi.service.FirebaseUserSyncService;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Spring Security filter that intercepts every request and, if a Firebase ID token is present
 * in the Authorization header, verifies it and populates the SecurityContext.
 *
 * Flow:
 *   1. Extract "Bearer <token>" from Authorization header.
 *   2. If Firebase Admin SDK is not initialised (dev without creds), skip silently.
 *   3. Verify the token via {@link FirebaseAuth#verifyIdToken(String)}.
 *   4. Sync the Firebase user to auth_accounts (find-or-create).
 *   5. Build a Spring Security principal with ROLE_<role> authority from the DB role,
 *      and any custom claims (teamId, etc.) for downstream use.
 *
 * Falls back to the existing JwtAuthenticationFilter seamlessly — both filters can
 * coexist in the filter chain, whichever sets the SecurityContext first wins.
 */
@Component
public class FirebaseTokenFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(FirebaseTokenFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final FirebaseUserSyncService syncService;

    public FirebaseTokenFilter(FirebaseUserSyncService syncService) {
        this.syncService = syncService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        // If the Authorization header doesn't look like a Firebase ID token (very long JWT),
        // skip this filter and let JwtAuthenticationFilter handle it.
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        // If Firebase Admin SDK is not initialised, skip gracefully.
        if (FirebaseApp.getApps().isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        // SecurityContext already set (e.g. by JwtAuthenticationFilter earlier in chain).
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        String idToken = authHeader.substring(BEARER_PREFIX.length()).trim();

        try {
            FirebaseToken decoded = FirebaseAuth.getInstance().verifyIdToken(idToken);
            AuthAccount account  = syncService.syncUser(decoded);

            // Reject deactivated accounts (e.g. admin suspended the user)
            if (Boolean.FALSE.equals(account.getIsActive())) {
                log.warn("Blocked sign-in for deactivated account: {}", decoded.getEmail());
                writeJsonError(response, HttpServletResponse.SC_FORBIDDEN,
                        "Your account has been deactivated. Contact your administrator.");
                return;
            }

            List<SimpleGrantedAuthority> authorities = List.of(
                    new SimpleGrantedAuthority("ROLE_" + account.getRole().name())
            );

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(account, null, authorities);

            SecurityContextHolder.getContext().setAuthentication(auth);
            log.debug("Firebase token verified for uid={} email={}", decoded.getUid(), decoded.getEmail());

        } catch (AccountNotInvitedException e) {
            // No pre-provisioned account exists for this email — invite-only enforcement
            log.warn("Firebase sign-in rejected — not invited: {}", e.getEmail());
            writeJsonError(response, HttpServletResponse.SC_FORBIDDEN, e.getMessage());
            return;

        } catch (FirebaseAuthException e) {
            log.warn("Firebase token verification failed: {}", e.getMessage());
            // Don't set authentication — Spring Security will return 401/403 as configured.
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Writes a JSON error body directly to the response and stops filter execution.
     * Filters run outside Spring MVC so {@code @RestControllerAdvice} won't handle them.
     */
    private void writeJsonError(HttpServletResponse response, int status, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"error\":\"" + message.replace("\"", "\\\"") + "\"}");
    }
}
