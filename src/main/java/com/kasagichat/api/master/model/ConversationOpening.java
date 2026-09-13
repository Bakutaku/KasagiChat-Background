package com.kasagichat.api.master.model;

import com.kasagichat.api.common.model.BaseTimeEntity;
import com.kasagichat.api.conversation.model.enums.ConversationScene;
import com.kasagichat.api.conversation.model.enums.ConversationType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 会話の冒頭の台詞と、その会話の方向性を管理するマスタEntity。
 *
 * <p>会話開始時にLLMを呼ばず、このマスタから抽選した台詞を1件目のメッセージとして保存する。
 * 方向性はシステムプロンプトへ差し込む。</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    name = "conversation_openings",
    indexes = @Index(name = "idx_conversation_openings_type_scene", columnList = "conversation_type, scene")
)
public class ConversationOpening extends BaseTimeEntity {

    /**
     * 冒頭の内部ID。
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 対象の会話種別。BIRTHまたはPRACTICE。
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ConversationType conversationType;

    /**
     * 対象の練習シーン。PRACTICEの場合のみ設定する。
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private ConversationScene scene;

    /**
     * 冒頭の台詞。
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String line;

    /**
     * 今回の会話の方向性。システムプロンプトへ差し込む。
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String theme;

    /**
     * 抽選対象かどうか。
     */
    @Builder.Default
    @Column(nullable = false)
    private Boolean enabled = true;
}
