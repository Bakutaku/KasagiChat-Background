package com.kasagichat.api.conversation.model;

import java.time.Instant;
import java.util.UUID;

import com.kasagichat.api.common.model.BaseTimeEntity;
import com.kasagichat.api.conversation.model.enums.ConversationScene;
import com.kasagichat.api.conversation.model.enums.ConversationStatus;
import com.kasagichat.api.conversation.model.enums.ConversationType;
import com.kasagichat.api.master.model.ConversationOpening;
import com.kasagichat.api.security.model.Users;

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
 * NPC誕生・練習・今日のひとことの会話と、その振り返り結果を保持するEntity。
 *
 * <p>振り返り済みの場合は保存済みの結果を返し、EXPなどを二重に反映しない。
 * 覚えた話題は {@code topics.source_conversation_id} から導出する。</p>
 *
 * <p>TODO: Flyway導入時に、同じユーザー・種別・シーンの未振り返り会話を1件に制限する部分ユニークインデックスを追加する。
 * それまではサービス側で保証する。</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    name = "conversations",
    uniqueConstraints = @UniqueConstraint(name = "uk_conversations_public_id", columnNames = "public_id"),
    indexes = @Index(name = "idx_conversations_user_status", columnList = "user_id, status")
)
public class Conversation extends BaseTimeEntity {

    /**
     * 会話の内部ID。
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * APIのパスで会話を識別する公開ID。
     */
    @Column(nullable = false, updatable = false)
    private UUID publicId;

    /**
     * 会話したユーザー。
     */
    // Usersは@SoftDeleteのため、LAZYにするとHibernateの起動に失敗する。
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    /**
     * 会話の種別。
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ConversationType type;

    /**
     * 練習シーン。PRACTICEの場合のみ設定する。
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private ConversationScene scene;

    /**
     * 会話の状態。
     */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ConversationStatus status = ConversationStatus.IN_PROGRESS;

    /**
     * ユーザーの発言とNPCの応答の往復数。
     */
    @Builder.Default
    @Column(nullable = false)
    private Integer turn = 0;

    /**
     * 冒頭に使用したマスタ。直前の会話と同じ冒頭を避けるために使う。
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opening_id")
    private ConversationOpening opening;

    /**
     * 今日のひとことで使用した質問。DAILYの場合のみ設定する。
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "daily_question_id")
    private DailyQuestion dailyQuestion;

    /**
     * 振り返りでユーザーへ伝えた褒めフィードバック。
     */
    @Column(columnDefinition = "TEXT")
    private String reviewFeedback;

    /**
     * 振り返りで獲得したEXP。
     */
    @Column
    private Integer reviewExpGained;

    /**
     * 振り返り後のNPCのレベル。
     */
    @Column
    private Integer reviewLevel;

    /**
     * 振り返りでレベルアップしたかどうか。
     */
    @Column
    private Boolean reviewLeveledUp;

    /**
     * 振り返りを実行した日時。未振り返りの場合はnull。
     */
    @Column
    private Instant reviewedAt;

    /**
     * 楽観ロック用のバージョン。メッセージ送信と振り返りの同時実行を防ぐ。
     */
    @Version
    private Long version;

    /**
     * 会話の公開IDを生成する。
     */
    @PrePersist
    void generatePublicId() {
        if (publicId == null) {
            publicId = UUID.randomUUID();
        }
    }
}
