package com.kasagichat.api.conversation.exception;

import org.springframework.http.HttpStatus;

import com.kasagichat.api.common.exception.BaseException;

/**
 * 会話開始に必要なマスタが設定されていない場合の例外。
 */
public final class ConversationConfigurationException extends BaseException {

    /**
     * 不足しているマスタの内容を示して例外を生成する。
     *
     * @param message 不足している設定の説明
     */
    public ConversationConfigurationException(String message) {
        super("CONVERSATION_CONFIGURATION_ERROR", HttpStatus.SERVICE_UNAVAILABLE, message);
    }
}
