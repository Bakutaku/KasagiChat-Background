package com.kasagichat.api.credential.model;

import com.kasagichat.api.common.model.BaseTimeEntity;
import com.kasagichat.api.credential.model.enums.LlmProvider;
import com.kasagichat.api.master.model.DemoPassphrase;
import com.kasagichat.api.security.model.Users;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ユーザーが設定したLLMプロバイダーとAPIキーを保持するEntity。
 *
 * <p>APIキーはアプリケーション側で暗号化してから保存し、復号済みのキーはクライアントへ返さない。</p>
 *
 * <p>TODO: Flyway導入時に「providerがDEMOの場合のみencrypted_api_keyがnull」のCHECK制約を追加する。</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    name = "api_credentials",
    uniqueConstraints = @UniqueConstraint(name = "uk_api_credentials_user_id", columnNames = "user_id")
)
public class ApiCredential extends BaseTimeEntity {

    /**
     * APIキー設定の内部ID。
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 設定を行ったユーザー。1ユーザーにつき1件。
     */
    // Usersは@SoftDeleteのため、LAZYにするとHibernateの起動に失敗する。
    @OneToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    /**
     * 使用するLLMプロバイダー。
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private LlmProvider provider;

    /**
     * 暗号化済みのAPIキー。IVと暗号文を連結して保存する。DEMOの場合はnull。
     */
    @Column
    private byte[] encryptedApiKey;

    /**
     * 画面表示用にマスクしたAPIキー。例: sk-...abcd。DEMOの場合はnull。
     */
    @Column(length = 30)
    private String maskedKey;

    /**
     * デモ利用を有効にした合言葉。DEMOの場合のみ設定する。
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "demo_passphrase_id")
    private DemoPassphrase demoPassphrase;
}
