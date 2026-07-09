package com.taskflow.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "profile_image_path")
    private String profileImagePath;

    /** Cloudinary public_id for the profile picture, used to delete/replace it. */
    @Column(name = "profile_image_public_id")
    private String profileImagePublicId;

    /**
     * Google account identifier ("sub" claim) linked to this user, if the
     * account was created or has ever signed in through "Continue with
     * Google". Null for accounts that have only ever used email/password.
     */
    @Column(name = "google_id", unique = true)
    private String googleId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /**
     * The user's last-used dashboard theme: "day" or "night". Defaults to
     * "day" for new accounts and for any existing account created before
     * this column existed (handled by {@link #getThemeMode()} below, since
     * ddl-auto=update leaves already-existing rows as NULL).
     */
    @Column(name = "theme_mode", length = 10)
    private String themeMode;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.themeMode == null) {
            this.themeMode = "day";
        }
    }

    /**
     * Never returns null, even for rows persisted before the theme_mode
     * column existed - Lombok's generated getter is intentionally
     * overridden here so every caller (templates included) always gets
     * either "day" or "night".
     */
    public String getThemeMode() {
        return themeMode == null ? "day" : themeMode;
    }
}
