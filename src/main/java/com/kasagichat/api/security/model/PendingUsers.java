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

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    uniqueConstraints = @UniqueConstraint(
        name = "uk_pending_users_provider_subject",
        columnNames = {"provider", "subject" } // providerとsubjectの組み合わせが一意であることを保証
    )
)
public class PendingUsers extends BaseTimeEntity{
    
    /**
     * 一時ユーザーID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 認証プロバイダー
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AuthProvider provider;

    /**
     * 認証プロバイダーが発行するユーザー識別子
     */
    @Column(nullable = false, length = 255)
    private String subject;

    /**
     * ユーザー名
     */
    @Column(nullable = false, length = 50)
    private String displayName;

    /**
     * アバターURL
     */
    @Column(length = 2048)
    private String avatarUrl;

    @Column(nullable = false)
    private Instant expiresAt;
}
