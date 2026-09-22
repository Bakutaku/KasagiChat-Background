package com.kasagichat.api.conversation.controller.dto.response;

import java.time.Instant;
import java.util.UUID;

import com.kasagichat.api.conversation.model.Conversation;
import com.kasagichat.api.conversation.model.enums.ConversationScene;
import com.kasagichat.api.conversation.model.enums.ConversationStatus;
import com.kasagichat.api.conversation.model.enums.ConversationType;
import com.kasagichat.api.conversation.service.ConversationPolicy;

/**
 * 会話一覧に表示する要約。メッセージ本文は含めない。
 *
 * <p>一覧の各行でメッセージを読み込むとN+1になるため、本文は詳細取得へ委ねる。</p>
 *
 * @param id 会話の公開ID
 * @param type 会話種別
 * @param scene 練習シーン
 * @param status 会話状態
 * @param turn 成功済みの往復数
 * @param canFinish 振り返りを実行できるか
 * @param startedAt 会話を開始した日時
 */
public record ConversationSummaryResponse(
    UUID id,
    ConversationType type,
    ConversationScene scene,
    ConversationStatus status,
    Integer turn,
    boolean canFinish,
    Instant startedAt
) {

    public static ConversationSummaryResponse from(Conversation conversation) {
        return new ConversationSummaryResponse(
            conversation.getPublicId(),
            conversation.getType(),
            conversation.getScene(),
            conversation.getStatus(),
            conversation.getTurn(),
            ConversationPolicy.of(conversation.getType())
                .canFinish(conversation.getTurn(), conversation.getStatus()),
            conversation.getCreatedAt()
        );
    }
}
