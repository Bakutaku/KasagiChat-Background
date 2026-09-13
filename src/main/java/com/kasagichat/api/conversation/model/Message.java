package com.kasagichat.api.conversation.model;

import com.kasagichat.api.common.model.BaseTimeEntity;
import com.kasagichat.api.conversation.model.enums.MessageRole;

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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 会話ログの1メッセージを保持するEntity。
 *
 * <p>成功した往復だけを保存する。原文は会話の再開・振り返りの入力・履歴表示にだけ使い、
 * 他の会話やイベントには渡さない。</p>
 *
 * <p>TODO: Flyway導入時にconversation_idへON DELETE CASCADEを追加する。</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    name = "messages",
    uniqueConstraints = @UniqueConstraint(name = "uk_messages_conversation_seq", columnNames = {"conversation_id", "seq"})
)
public class Message extends BaseTimeEntity {

    /**
     * メッセージの内部ID。
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * メッセージが属する会話。
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    /**
     * 会話内での連番。1件目は冒頭の台詞。
     */
    @Column(nullable = false)
    private Integer seq;

    /**
     * 発言者。
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MessageRole role;

    /**
     * メッセージ本文。
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;
}
