package com.kasagichat.api.event.model;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.kasagichat.api.common.model.BaseTimeEntity;
import com.kasagichat.api.security.model.Users;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * イベントのマッチングで受取人に届く出会いカードを保持するEntity。
 *
 * <p>共通タグはマッチング時点の記録とする。会話報告は開封時に、その時点の公開話題から生成して保存し、
 * 以後は再生成しない。</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    name = "cards",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_cards_public_id", columnNames = "public_id"),
        @UniqueConstraint(
            name = "uk_cards_event_recipient_partner",
            columnNames = {"event_id", "recipient_user_id", "partner_user_id"}
        )
    },
    indexes = {
        @Index(name = "idx_cards_recipient_user_id", columnList = "recipient_user_id"),
        @Index(name = "idx_cards_partner_user_id", columnList = "partner_user_id")
    }
)
public class Card extends BaseTimeEntity {

    /**
     * カードの内部ID。
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * APIのパスでカードを識別する公開ID。
     */
    @Column(nullable = false, updatable = false)
    private UUID publicId;

    /**
     * カードが作られたイベント。
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    /**
     * カードを受け取るユーザー。
     */
    // Usersは@SoftDeleteのため、LAZYにするとHibernateの起動に失敗する。
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "recipient_user_id", nullable = false)
    private Users recipient;

    /**
     * カードに載る相手のユーザー。
     */
    // Usersは@SoftDeleteのため、LAZYにするとHibernateの起動に失敗する。
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "partner_user_id", nullable = false)
    private Users partner;

    /**
     * 相性スコア。公開話題のカテゴリ一致から機械的に算出する。
     */
    @Column(nullable = false)
    private Integer score;

    /**
     * カード表面に表示する共通タグ。
     */
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(nullable = false, columnDefinition = "text[]")
    private String[] commonTags;

    /**
     * カードを開封した日時。未開封の場合はnull。
     */
    @Column
    private Instant openedAt;

    /**
     * 開封時に生成した会話報告。
     */
    @Column(columnDefinition = "TEXT")
    private String report;

    /**
     * 開封時に生成したおすすめ話題。
     */
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "text[]")
    private String[] recommendedTopics;

    /**
     * 楽観ロック用のバージョン。開封の同時実行による二重生成を防ぐ。
     */
    @Version
    private Long version;

    /**
     * カードの公開IDを生成する。
     */
    @PrePersist
    void generatePublicId() {
        if (publicId == null) {
            publicId = UUID.randomUUID();
        }
    }
}
