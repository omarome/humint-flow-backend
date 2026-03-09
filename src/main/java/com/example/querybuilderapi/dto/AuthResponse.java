package com.example.querybuilderapi.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Response DTO returned after successful login/register/refresh.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {

    private String accessToken;
    private String refreshToken;
    private long expiresIn;   // seconds until access token expires
    private String tokenType = "Bearer";
    private UserInfo user;

    public AuthResponse() {}

    public AuthResponse(String accessToken, String refreshToken, long expiresIn, UserInfo user) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
        this.user = user;
    }

    // --- Getters & Setters ---

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

    public long getExpiresIn() { return expiresIn; }
    public void setExpiresIn(long expiresIn) { this.expiresIn = expiresIn; }

    public String getTokenType() { return tokenType; }
    public void setTokenType(String tokenType) { this.tokenType = tokenType; }

    public UserInfo getUser() { return user; }
    public void setUser(UserInfo user) { this.user = user; }

    /**
     * Subset of user info returned in auth responses.
     */
    public static class UserInfo {
        private Long id;
        private String email;
        private String displayName;
        private String role;

        public UserInfo() {}

        public UserInfo(Long id, String email, String displayName, String role) {
            this.id = id;
            this.email = email;
            this.displayName = displayName;
            this.role = role;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
    }
}
