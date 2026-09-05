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
 * 外部認証テーブル
 * 外部認証サービスのアカウントとユーザーの紐付けを保持するクラス。
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
        columnNames = {"provider", "subject" } // providerとsubjectの組み合わせが一意であることを保証
    ), 
    indexes = @Index(name = "idx_user_auth_user_id", columnList = "user_id")
)
public class UserAuth extends BaseTimeEntity {

    /**
     * ユーザー認証ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * ユーザーID
     */
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

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
}
