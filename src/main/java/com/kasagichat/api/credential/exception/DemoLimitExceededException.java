package com.kasagichat.api.credential.exception;

import org.springframework.http.HttpStatus;

import com.kasagichat.api.common.exception.BaseException;

/**
 * デモLLMのアカウント単位の呼び出し上限に達した場合の例外。
 */
public final class DemoLimitExceededException extends BaseException {

    public DemoLimitExceededException() {
        super(
            "DEMO_LIMIT_EXCEEDED",
            HttpStatus.TOO_MANY_REQUESTS,
            "デモAIの利用上限に達しました。"
        );
    }
}
