package com.kasagichat.api.credential.exception;

import org.springframework.http.HttpStatus;

import com.kasagichat.api.common.exception.BaseException;

/**
 * 暗号化鍵または運営LLM設定が不足している場合の例外。
 */
public class LlmConfigurationException extends BaseException {

    public LlmConfigurationException(String message) {
        super("LLM_CONFIGURATION_ERROR", HttpStatus.SERVICE_UNAVAILABLE, message);
    }
}
