package com.kasagichat.api.credential.exception;

import org.springframework.http.HttpStatus;

import com.kasagichat.api.common.exception.BaseException;

/**
 * LLMプロバイダーの呼び出しに失敗した場合の例外。
 */
public final class LlmCallFailedException extends BaseException {

    public LlmCallFailedException() {
        super(
            "LLM_CALL_FAILED",
            HttpStatus.BAD_GATEWAY,
            "AIの応答を取得できませんでした。時間をおいて再試行してください。"
        );
    }
}
