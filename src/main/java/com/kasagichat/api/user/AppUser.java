package com.kasagichat.api.user;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * ユーザー。OAuthプロバイダ + subject の組で一意に識別する(requirements.md データモデル準拠)。
 */
@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(columnNames = { "provider", "subject" }))
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 32)
    private String provider;

    @Column(nullable = false, length = 255)
    private String subject;

    @Column(nullable = false, length = 100)
    private String displayName;

    @Column(length = 500)
    private String avatarUrl;

    @Column(nullable = false)
    private Instant createdAt;

    /** 利用規約同意日時(初回フロー実装時に使用。未同意はnull) */
    private Instant termsAgreedAt;

    protected AppUser() {
    }

    public AppUser(String provider, String subject, String displayName, String avatarUrl) {
        this.provider = provider;
        this.subject = subject;
        this.displayName = displayName;
        this.avatarUrl = avatarUrl;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getProvider() {
        return provider;
    }

    public String getSubject() {
        return subject;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getTermsAgreedAt() {
        return termsAgreedAt;
    }

    public void setTermsAgreedAt(Instant termsAgreedAt) {
        this.termsAgreedAt = termsAgreedAt;
    }
}
