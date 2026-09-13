package com.kasagichat.api.master.model;

import com.kasagichat.api.common.model.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * デモ利用を有効にする合言葉を管理するマスタEntity。
 *
 * <p>漏洩した場合は無効化し、新しい合言葉を追加して差し替える。</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    name = "demo_passphrases",
    uniqueConstraints = @UniqueConstraint(name = "uk_demo_passphrases_passphrase", columnNames = "passphrase")
)
public class DemoPassphrase extends BaseTimeEntity {

    /**
     * 合言葉の内部ID。
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 合言葉。
     */
    @Column(nullable = false, length = 50)
    private String passphrase;

    /**
     * アカウントごとのLLM呼び出し回数の上限。
     */
    @Column(nullable = false)
    private Integer callLimit;

    /**
     * 合言葉が有効かどうか。
     */
    @Builder.Default
    @Column(nullable = false)
    private Boolean enabled = true;
}
