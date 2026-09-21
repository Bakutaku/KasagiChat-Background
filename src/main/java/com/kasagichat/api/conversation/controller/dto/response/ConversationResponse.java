package com.kasagichat.api.conversation.controller.dto.response;

import java.util.List;
import java.util.UUID;

import com.kasagichat.api.conversation.model.Conversation;
import com.kasagichat.api.conversation.model.Message;
import com.kasagichat.api.conversation.model.enums.ConversationScene;
import com.kasagichat.api.conversation.model.enums.ConversationStatus;
import com.kasagichat.api.conversation.model.enums.ConversationType;

/**
 * 会話の現在状態と保存済みログ。
 *
 * @param id 会話の公開ID
 * @param type 会話種別
 * @param scene 練習シーン
 * @param status 会話状態
 * @param turn 成功済みの往復数
 * @param canFinish 振り返りを実行できるか
 * @param messages 保存済みメッセージ
 */
public record ConversationResponse(
    UUID id,
    ConversationType type,
    ConversationScene scene,
    ConversationStatus status,
    Integer turn,
    boolean canFinish,
    List<ConversationMessageResponse> messages
) {

    public static ConversationResponse from(Conversation conversation, List<Message> messages) {
        return new ConversationResponse(
            conversation.getPublicId(),
            conversation.getType(),
            conversation.getScene(),
            conversation.getStatus(),
            conversation.getTurn(),
            conversation.getTurn() >= 3 && conversation.getStatus() != ConversationStatus.REVIEWED,
            messages.stream().map(ConversationMessageResponse::from).toList()
        );
    }
}
