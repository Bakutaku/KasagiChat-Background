package com.kasagichat.api.npc.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.kasagichat.api.common.model.BaseTimeEntity;
import com.kasagichat.api.conversation.model.Conversation;
import com.kasagichat.api.master.model.TopicCategory;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * NPCが覚えた話題を保持するEntity。
 *
 * <p>カテゴリ付きの話題から家の思い出の品を導出する。削除はプライバシーのため物理削除とし、
 * 所属する思い出も一緒に削除する。</p>
 *
 * <p>公開/非公開はユーザーが決める。未確認の話題は非公開として扱い、イベントでは使わない。</p>
 *
 * <p>TODO: Flyway導入時にsource_conversation_idへON DELETE SET NULLを追加する。</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    name = "topics",
    uniqueConstraints = @UniqueConstraint(name = "uk_topics_npc_name", columnNames = {"npc_id", "name"}),
    indexes = {
        @Index(name = "idx_topics_category_id", columnList = "category_id"),
        @Index(name = "idx_topics_source_conversation_id", columnList = "source_conversation_id")
    }
)
public class Topic extends BaseTimeEntity {

    /**
     * 話題の内部ID。
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 話題を覚えたNPC。
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "npc_id", nullable = false)
    private Npc npc;

    /**
     * 話題名。
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * 話題のカテゴリ。どのカテゴリにも入らない場合はnullとし、本棚に置く。
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private TopicCategory category;

    /**
     * 話題への興味度。マッチングの重み付けに使う。
     */
    @Column(nullable = false)
    private Short interest;

    /**
     * イベントのマッチングとカード生成に使ってよいかどうか。
     */
    @Builder.Default
    @Column(name = "is_public", nullable = false)
    private Boolean publicTopic = false;

    /**
     * ユーザーが公開/非公開を決めた日時。未確認の場合はnull。
     */
    @Column
    private Instant visibilityDecidedAt;

    /**
     * 話題を覚えた日時。思い出の品の入手日として表示する。
     */
    @Column(nullable = false)
    private Instant learnedAt;

    /**
     * 話題を覚えた会話。
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_conversation_id")
    private Conversation sourceConversation;

    /**
     * 話題に属する思い出。話題を削除すると一緒に削除する。
     */
    @Builder.Default
    @OneToMany(mappedBy = "topic", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<Memory> memories = new ArrayList<>();
}
