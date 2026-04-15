package com.example.querybuilderapi.service;

import com.example.querybuilderapi.dto.AuthResponse;
import com.example.querybuilderapi.dto.LoginRequest;
import com.example.querybuilderapi.dto.RegisterRequest;
import com.example.querybuilderapi.exception.AccountNotInvitedException;
import com.example.querybuilderapi.model.AuthAccount;
import com.example.querybuilderapi.model.RefreshToken;
import com.example.querybuilderapi.repository.AuthAccountRepository;
import com.example.querybuilderapi.repository.RefreshTokenRepository;
import com.example.querybuilderapi.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Authentication business logic: register, login, refresh, logout.
 */
@Service
public class AuthService {

    private final AuthAccountRepository authAccountRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final long refreshTokenExpiryMs;

    public AuthService(AuthAccountRepository authAccountRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       JwtService jwtService,
                       PasswordEncoder passwordEncoder,
                       @org.springframework.beans.factory.annotation.Value("${app.jwt.refresh-token-expiry-ms:604800000}")
                       long refreshTokenExpiryMs) {
        this.authAccountRepository = authAccountRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenExpiryMs = refreshTokenExpiryMs;
    }

    /**
     * Complete account setup for a user who was pre-provisioned via the invite flow.
     *
     * The caller must already have a pending {@code auth_accounts} row created by
     * {@code POST /api/admin/invite} (i.e. {@code is_active = false}, no password hash).
     * This endpoint sets the password and activates the account so the user can log in.
     *
     * Callers with an email that has NOT been pre-provisioned receive
     * {@link AccountNotInvitedException} (→ 403) — they cannot self-register.
     *
     * @throws AccountNotInvitedException if no pending invite exists for the email
     * @throws IllegalArgumentException   if the email is already active (account already set up)
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        AuthAccount account = authAccountRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AccountNotInvitedException(request.getEmail()));

        // Guard: account already active means the invite was previously accepted.
        if (Boolean.TRUE.equals(account.getIsActive())) {
            throw new IllegalArgumentException(
                    "An active account already exists for '" + request.getEmail() + "'. "
                    + "Use the login endpoint instead.");
        }

        // Accept the invite: set password and activate the account.
        account.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        account.setDisplayName(request.getDisplayName());
        account.setOauthProvider(AuthAccount.OAuthProvider.LOCAL);
        account.setIsActive(true);
        account = authAccountRepository.save(account);

        return buildAuthResponse(account);
    }

    /**
     * Authenticate with email + password.
     */
    public AuthResponse login(LoginRequest request) {
        AuthAccount account = authAccountRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (account.getPasswordHash() == null
                || !passwordEncoder.matches(request.getPassword(), account.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        return buildAuthResponse(account);
    }

    /**
     * Exchange a valid refresh token for a new token pair.
     */
    @Transactional
    public AuthResponse refresh(String refreshTokenValue) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        if (refreshToken.isRevoked() || refreshToken.isExpired()) {
            throw new IllegalArgumentException("Refresh token expired or revoked");
        }

        // Revoke the old refresh token (rotation)
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        // Issue new token pair
        return buildAuthResponse(refreshToken.getAuthAccount());
    }

    /**
     * Revoke all refresh tokens for a given account (logout).
     */
    @Transactional
    public void logout(String refreshTokenValue) {
        refreshTokenRepository.findByToken(refreshTokenValue)
                .ifPresent(rt -> {
                    rt.setRevoked(true);
                    refreshTokenRepository.save(rt);
                });
    }

    /**
     * Handles an OAuth2 (Google) login via Spring Security's OAuth2 client.
     *
     * Invite-only enforcement: if no pre-provisioned account exists for the
     * email, an {@link AccountNotInvitedException} is thrown (→ 403).
     * Auto-creating accounts for new OAuth users is intentionally disabled.
     */
    @Transactional
    public AuthResponse handleOAuthLogin(String email, String displayName,
                                          AuthAccount.OAuthProvider provider, String oauthId, String photoUrl) {
        AuthAccount account = authAccountRepository
                // Fast path: already linked by provider + oauthId
                .findByOauthProviderAndOauthId(provider, oauthId)
                .map(existing -> {
                    if (photoUrl != null) existing.setPhotoUrl(photoUrl);
                    return authAccountRepository.save(existing);
                })
                .orElseGet(() -> {
                    // Link to a pre-provisioned (invited) account by email
                    AuthAccount invited = authAccountRepository.findByEmail(email)
                            .orElseThrow(() -> new AccountNotInvitedException(email));

                    invited.setOauthProvider(provider);
                    invited.setOauthId(oauthId);
                    invited.setIsActive(true);      // activate pending invite
                    if (photoUrl != null && invited.getPhotoUrl() == null) {
                        invited.setPhotoUrl(photoUrl);
                    }
                    return authAccountRepository.save(invited);
                });

        return buildAuthResponse(account);
    }

    /**
     * Update an account's display name.
     */
    @Transactional
    public AuthResponse.UserInfo updateProfile(Long accountId, String displayName) {
        AuthAccount account = authAccountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        account.setDisplayName(displayName);
        account = authAccountRepository.save(account);

        return new AuthResponse.UserInfo(
                account.getId(),
                account.getEmail(),
                account.getDisplayName(),
                account.getRole().name(),
                account.getPhotoUrl()
        );
    }

    /**
     * Permanently delete an account and its refresh tokens.
     */
    @Transactional
    public void deleteAccount(Long accountId) {
        if (!authAccountRepository.existsById(accountId)) {
            throw new IllegalArgumentException("Account not found");
        }
        refreshTokenRepository.deleteByAuthAccountId(accountId);
        authAccountRepository.deleteById(accountId);
    }

    // ── helpers ──────────────────────────────────────────────

    private AuthResponse buildAuthResponse(AuthAccount account) {
        String accessToken = jwtService.generateAccessToken(account);
        String refreshTokenValue = createRefreshToken(account);

        AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo(
                account.getId(),
                account.getEmail(),
                account.getDisplayName(),
                account.getRole().name(),
                account.getPhotoUrl()
        );

        return new AuthResponse(
                accessToken,
                refreshTokenValue,
                jwtService.getAccessTokenExpirySeconds(),
                userInfo
        );
    }

    private String createRefreshToken(AuthAccount account) {
        String tokenValue = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plusMillis(refreshTokenExpiryMs);

        RefreshToken refreshToken = new RefreshToken(tokenValue, account, expiresAt);
        refreshTokenRepository.save(refreshToken);

        return tokenValue;
    }
}
