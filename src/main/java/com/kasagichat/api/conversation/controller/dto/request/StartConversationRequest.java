package com.kasagichat.api.conversation.controller.dto.request;

import com.kasagichat.api.conversation.model.enums.ConversationScene;
import com.kasagichat.api.conversation.model.enums.ConversationType;

import jakarta.validation.constraints.NotNull;

/**
 * 会話の開始・再開条件。
 *
 * @param type 会話種別
 * @param scene 練習シーン。PRACTICEのみ指定し、BIRTHとDAILYではnull
 */
public record StartConversationRequest(
    @NotNull ConversationType type,
    ConversationScene scene
) {
}
