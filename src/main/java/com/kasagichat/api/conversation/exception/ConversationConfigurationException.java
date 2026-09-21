package com.kasagichat.api.conversation.exception;

import org.springframework.http.HttpStatus;

import com.kasagichat.api.common.exception.BaseException;

/**
 * 会話開始に必要なマスタが設定されていない場合の例外。
 */
public final class ConversationConfigurationException extends BaseException {

    public ConversationConfigurationException() {
        super(
            "CONVERSATION_CONFIGURATION_ERROR",
            HttpStatus.SERVICE_UNAVAILABLE,
            "NPC誕生会話の開始設定がありません。"
        );
    }
}
