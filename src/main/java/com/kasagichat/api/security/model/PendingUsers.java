package com.kasagichat.api.security.model;

import java.time.Instant;

import com.kasagichat.api.common.model.BaseTimeEntity;
import com.kasagichat.api.security.model.enums.AuthProvider;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * OAuth認証済みで本登録前のユーザー情報を保持するEntity。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    uniqueConstraints = @UniqueConstraint(
        name = "uk_pending_users_provider_subject",
        columnNames = {"provider", "subject" } // providerとsubjectの組み合わせが一意であることを保証する。
    )
)
public class PendingUsers extends BaseTimeEntity{
    
    /**
     * 仮登録ユーザーの内部ID。
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * OAuth認証に使用したプロバイダー。
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AuthProvider provider;

    /**
     * OAuthプロバイダー内で一意なユーザー識別子。
     */
    @Column(nullable = false, length = 255)
    private String subject;

    /**
     * 本登録時に使用する表示名の候補。
     */
    @Column(nullable = false, length = 50)
    private String displayName;

    /**
     * 本登録時に引き継ぐアバター画像URL。
     */
    @Column(length = 2048)
    private String avatarUrl;

    /**
     * 仮登録情報の有効期限。
     */
    @Column(nullable = false)
    private Instant expiresAt;
}
