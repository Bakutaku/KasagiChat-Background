package com.kasagichat.api.conversation.exception;

import org.springframework.http.HttpStatus;

import com.kasagichat.api.common.exception.BaseException;

/**
 * 終了済みの会話へ発言しようとした場合の例外。
 */
public final class ConversationFinishedException extends BaseException {

    public ConversationFinishedException() {
        super("CONVERSATION_FINISHED", HttpStatus.CONFLICT, "この会話は終了しています。");
    }
}
