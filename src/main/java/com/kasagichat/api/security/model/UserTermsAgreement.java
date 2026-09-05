package com.kasagichat.api.security.model;

import java.time.Instant;

import com.kasagichat.api.common.model.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
 * ユーザーが同意した規約と同意日時を保持するEntity。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    uniqueConstraints = @UniqueConstraint(
        name = "uk_user_terms_agreement_user_terms",
        columnNames = {"user_id", "terms_id"}
    ),
    indexes = {
        @jakarta.persistence.Index(name = "idx_user_terms_agreement_terms_id", columnList = "terms_id")
    }
)
public class UserTermsAgreement extends BaseTimeEntity{
    
    /**
     * 規約同意履歴の内部ID。
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 規約へ同意したユーザー。
     */
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    /**
     * ユーザーが同意した規約。
     */
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "terms_id", nullable = false)
    private Terms terms;

    /**
     * ユーザーが規約へ同意した日時。
     */
    @Column(nullable = false)
    private Instant agreedAt;
}
