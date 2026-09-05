package com.kasagichat.api.security.model;

import com.kasagichat.api.common.model.BaseTimeEntity;
import com.kasagichat.api.security.model.enums.AuthProvider;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * OAuthアカウントと本登録済みユーザーの紐付けを保持するEntity。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    uniqueConstraints = @UniqueConstraint(
        name = "uk_user_auth_provider_subject",
        columnNames = {"provider", "subject" } // providerとsubjectの組み合わせが一意であることを保証する。
    ), 
    indexes = @Index(name = "idx_user_auth_user_id", columnList = "user_id")
)
public class UserAuth extends BaseTimeEntity {

    /**
     * 外部認証情報の内部ID。
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * OAuthアカウントに紐付くユーザー。
     */
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    /**
     * OAuth認証に使用するプロバイダー。
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AuthProvider provider;

    /**
     * OAuthプロバイダー内で一意なユーザー識別子。
     */
    @Column(nullable = false, length = 255)
    private String subject;
}
