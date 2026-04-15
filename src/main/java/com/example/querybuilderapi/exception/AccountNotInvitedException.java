package com.example.querybuilderapi.exception;

/**
 * Thrown when a Firebase (or OAuth) user attempts to sign in but no
 * pre-provisioned {@code auth_accounts} record exists for their email.
 *
 * This replaces the old behaviour of auto-creating a SALES_REP account on
 * first sign-in.  Accounts must now be created in advance by an ADMIN via
 * {@code POST /api/admin/invite} before the user can authenticate.
 */
public class AccountNotInvitedException extends RuntimeException {

    private final String email;

    public AccountNotInvitedException(String email) {
        super("No account found for '" + email
                + "'. Ask your administrator to send you an invitation.");
        this.email = email;
    }

    public String getEmail() {
        return email;
    }
}
