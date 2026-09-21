package com.kasagichat.api.conversation.exception;

import org.springframework.http.HttpStatus;

import com.kasagichat.api.common.exception.BaseException;

/**
 * 会話が存在しない、または本人の会話ではない場合の例外。
 */
public final class ConversationNotFoundException extends BaseException {

    public ConversationNotFoundException() {
        super("CONVERSATION_NOT_FOUND", HttpStatus.NOT_FOUND, "会話が見つかりませんでした。");
    }
}
