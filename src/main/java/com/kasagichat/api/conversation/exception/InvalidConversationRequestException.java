package com.kasagichat.api.conversation.exception;

import org.springframework.http.HttpStatus;

import com.kasagichat.api.common.exception.BaseException;

/**
 * 未実装の会話種別や不正な種別・シーンの組み合わせを受け取った場合の例外。
 */
public final class InvalidConversationRequestException extends BaseException {

    public InvalidConversationRequestException(String message) {
        super("INVALID_CONVERSATION_REQUEST", HttpStatus.BAD_REQUEST, message);
    }
}
