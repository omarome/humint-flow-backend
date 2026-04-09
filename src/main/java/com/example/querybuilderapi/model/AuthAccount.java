package com.example.querybuilderapi.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.time.Instant;


/**
 * Authentication account entity.
 * Separate from the domain User entity — this handles login credentials.
 */
@Entity
@Table(name = "auth_accounts")
public class AuthAccount {

    public enum Role { USER, ADMIN, MANAGER, SALES_REP }
    public enum OAuthProvider { LOCAL, GOOGLE, GITHUB, FIREBASE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    /** BCrypt-hashed password. Null for pure OAuth accounts. */
    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "display_name", nullable = false)
    @NotBlank(message = "Display name is required")
    @Pattern(regexp = "^[\\p{L}0-9 _.'-]+$", message = "Display name contains invalid characters")
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USER;

    @Enumerated(EnumType.STRING)
    @Column(name = "oauth_provider", nullable = false)
    private OAuthProvider oauthProvider = OAuthProvider.LOCAL;

    /** External OAuth ID (e.g. Google sub or GitHub id). */
    @Column(name = "oauth_id")
    private String oauthId;

    @Column(name = "photo_url")
    private String photoUrl;

    // ─── Firebase Integration Fields ──────────────────────────────────────

    /** Firebase UID — the unique identifier from Firebase Authentication. */
    @Column(name = "firebase_uid", unique = true, length = 128)
    private String firebaseUid;

    /** FCM registration token for push notifications to this user's device. */
    @Column(name = "fcm_token", columnDefinition = "TEXT")
    private String fcmToken;

    /** Timestamp of last FCM token update — used to detect stale tokens. */
    @Column(name = "fcm_token_updated_at")
    private Instant fcmTokenUpdatedAt;

    // ─── Team Member Profile Fields ───────────────────────────────────────

    @Column(name = "job_title", length = 150)
    private String jobTitle;

    @Column(name = "department", length = 100)
    private String department = "Sales";

    @Column(name = "phone", length = 50)
    private String phone;

    @Column(name = "avatar_url", columnDefinition = "TEXT")
    private String avatarUrl;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    /** Self-referencing FK to manager's auth_accounts row. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private AuthAccount manager;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    // --- Constructors ---

    public AuthAccount() {}

    public AuthAccount(String email, String passwordHash, String displayName,
                       Role role, OAuthProvider oauthProvider, String oauthId, String photoUrl) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.role = role;
        this.oauthProvider = oauthProvider;
        this.oauthId = oauthId;
        this.photoUrl = photoUrl;
    }

    public AuthAccount(String email, String firebaseUid, String displayName, String photoUrl) {
        this.email = email;
        this.firebaseUid = firebaseUid;
        this.displayName = displayName;
        this.photoUrl = photoUrl;
        this.oauthProvider = OAuthProvider.FIREBASE;
        this.role = Role.SALES_REP;
    }

    // --- Getters & Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public OAuthProvider getOauthProvider() { return oauthProvider; }
    public void setOauthProvider(OAuthProvider oauthProvider) { this.oauthProvider = oauthProvider; }

    public String getOauthId() { return oauthId; }
    public void setOauthId(String oauthId) { this.oauthId = oauthId; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public String getFirebaseUid() { return firebaseUid; }
    public void setFirebaseUid(String firebaseUid) { this.firebaseUid = firebaseUid; }

    public String getFcmToken() { return fcmToken; }
    public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }

    public Instant getFcmTokenUpdatedAt() { return fcmTokenUpdatedAt; }
    public void setFcmTokenUpdatedAt(Instant fcmTokenUpdatedAt) { this.fcmTokenUpdatedAt = fcmTokenUpdatedAt; }

    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public AuthAccount getManager() { return manager; }
    public void setManager(AuthAccount manager) { this.manager = manager; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
