package com.example.querybuilderapi.service;

import com.example.querybuilderapi.dto.AuthResponse;
import com.example.querybuilderapi.dto.LoginRequest;
import com.example.querybuilderapi.dto.RegisterRequest;
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
     * Register a new local account.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (authAccountRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }

        AuthAccount account = new AuthAccount(
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                request.getDisplayName(),
                AuthAccount.Role.USER,
                AuthAccount.OAuthProvider.LOCAL,
                null
        );
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
     * Find or create an account from an OAuth2 login.
     */
    @Transactional
    public AuthResponse handleOAuthLogin(String email, String displayName,
                                          AuthAccount.OAuthProvider provider, String oauthId) {
        AuthAccount account = authAccountRepository
                .findByOauthProviderAndOauthId(provider, oauthId)
                .orElseGet(() -> {
                    // Check if a local account with this email exists — link it
                    AuthAccount existing = authAccountRepository.findByEmail(email).orElse(null);
                    if (existing != null) {
                        existing.setOauthProvider(provider);
                        existing.setOauthId(oauthId);
                        return authAccountRepository.save(existing);
                    }
                    // Create a brand-new OAuth account
                    AuthAccount newAccount = new AuthAccount(
                            email, null, displayName,
                            AuthAccount.Role.USER, provider, oauthId
                    );
                    return authAccountRepository.save(newAccount);
                });

        return buildAuthResponse(account);
    }

    // ── helpers ──────────────────────────────────────────────

    private AuthResponse buildAuthResponse(AuthAccount account) {
        String accessToken = jwtService.generateAccessToken(account);
        String refreshTokenValue = createRefreshToken(account);

        AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo(
                account.getId(),
                account.getEmail(),
                account.getDisplayName(),
                account.getRole().name()
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
