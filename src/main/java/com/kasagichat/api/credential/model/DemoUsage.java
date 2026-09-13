package com.kasagichat.api.credential.model;

import java.time.Instant;

import com.kasagichat.api.common.model.BaseTimeEntity;
import com.kasagichat.api.security.model.Users;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * デモ利用者のLLM呼び出し回数をアカウント単位で保持するEntity。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    name = "demo_usages",
    uniqueConstraints = @UniqueConstraint(name = "uk_demo_usages_user_id", columnNames = "user_id")
)
public class DemoUsage extends BaseTimeEntity {

    /**
     * デモ利用回数の内部ID。
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * デモを利用したユーザー。1ユーザーにつき1件。
     */
    // Usersは@SoftDeleteのため、LAZYにするとHibernateの起動に失敗する。
    @OneToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    /**
     * これまでのLLM呼び出し回数。
     */
    @Builder.Default
    @Column(nullable = false)
    private Integer callCount = 0;

    /**
     * 最後にLLMを呼び出した日時。
     */
    @Column
    private Instant lastCalledAt;
}
