package com.kasagichat.api.token;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * リフレッシュトークン(不透明トークン)。
 *
 * トークン本体は保存せず**SHA-256ハッシュのみ**保存する。
 * DBが漏洩してもハッシュから元のトークンは復元できないため、なりすましに使えない
 * (パスワードをハッシュで保存するのと同じ発想。こちらは高エントロピー乱数なので
 * 総当たりが不可能であり、bcryptのような低速ハッシュは不要)。
 */
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private Instant createdAt;

    protected RefreshToken() {
    }

    public RefreshToken(Long userId, String tokenHash, Instant expiresAt) {
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.createdAt = Instant.now();
    }

    public Long getUserId() {
        return userId;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}
