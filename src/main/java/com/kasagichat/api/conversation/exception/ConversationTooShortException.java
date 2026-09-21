package com.kasagichat.api.conversation.exception;

import org.springframework.http.HttpStatus;

import com.kasagichat.api.common.exception.BaseException;

/**
 * 振り返りに必要な往復数へ達していない場合の例外。
 */
public final class ConversationTooShortException extends BaseException {

    public ConversationTooShortException() {
        super(
            "CONVERSATION_TOO_SHORT",
            HttpStatus.BAD_REQUEST,
            "NPC誕生の振り返りには3往復以上の会話が必要です。"
        );
    }
}
