package com.kasagichat.api.conversation.controller.dto.response;

import com.kasagichat.api.conversation.model.Message;
import com.kasagichat.api.conversation.model.enums.MessageRole;

/**
 * 会話ログの1メッセージ。
 *
 * @param role 発言者
 * @param text 本文
 */
public record ConversationMessageResponse(
    MessageRole role,
    String text
) {

    public static ConversationMessageResponse from(Message message) {
        return new ConversationMessageResponse(message.getRole(), message.getText());
    }
}
