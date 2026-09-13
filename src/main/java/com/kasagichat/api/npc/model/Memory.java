package com.kasagichat.api.npc.model;

import java.time.Instant;

import com.kasagichat.api.common.model.BaseTimeEntity;
import com.kasagichat.api.conversation.model.Conversation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 会話で分かった具体的な出来事を、話題にぶら下げて保持するEntity。
 *
 * <p>公開/非公開と削除は所属する話題に従う。会話ログの原文の代わりに、
 * 次の会話やカード生成のプロンプトへ入れる。</p>
 *
 * <p>TODO: Flyway導入時にtopic_idへON DELETE CASCADE、source_conversation_idへON DELETE SET NULLを追加する。</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    name = "memories",
    indexes = {
        @Index(name = "idx_memories_topic_occurred_at", columnList = "topic_id, occurred_at"),
        @Index(name = "idx_memories_source_conversation_id", columnList = "source_conversation_id")
    }
)
public class Memory extends BaseTimeEntity {

    /**
     * 思い出の内部ID。
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 所属する話題。
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;

    /**
     * 思い出を抽出した会話。
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_conversation_id")
    private Conversation sourceConversation;

    /**
     * 具体的な出来事の要約。1〜3文程度。
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * 出来事がいつの話か。分からない場合はnull。
     */
    @Column
    private Instant occurredAt;
}
