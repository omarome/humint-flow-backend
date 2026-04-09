package com.example.querybuilderapi.service;

import com.example.querybuilderapi.model.AuthAccount;
import com.example.querybuilderapi.repository.AuthAccountRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Utility service to extract the current authenticated user's ID
 * from the Spring Security context.
 *
 * Used to auto-populate createdBy / updatedBy audit fields
 * on all CRM entities that extend BaseEntity.
 */
@Service
public class AuditAwareService {

    private final AuthAccountRepository authAccountRepository;

    public AuditAwareService(AuthAccountRepository authAccountRepository) {
        this.authAccountRepository = authAccountRepository;
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

        // The principal is the email set by JwtAuthenticationFilter
        Optional<AuthAccount> account = authAccountRepository.findByEmail(principal);
        return account.map(AuthAccount::getId).orElse(null);
    }
}
