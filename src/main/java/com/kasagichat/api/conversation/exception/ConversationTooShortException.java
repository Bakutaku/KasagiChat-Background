package com.kasagichat.api.conversation.exception;

import org.springframework.http.HttpStatus;

import com.kasagichat.api.common.exception.BaseException;

/**
 * 振り返りに必要な往復数へ達していない場合の例外。
 */
public final class ConversationTooShortException extends BaseException {

    /**
     * 必要な往復数を示して例外を生成する。
     *
     * @param requiredTurns 振り返りに必要な往復数
     */
    public ConversationTooShortException(int requiredTurns) {
        super(
            "CONVERSATION_TOO_SHORT",
            HttpStatus.BAD_REQUEST,
            "振り返りには%d往復以上の会話が必要です。".formatted(requiredTurns)
        );
    }
}
