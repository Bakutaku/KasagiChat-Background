package com.kasagichat.api.credential.exception;

import org.springframework.http.HttpStatus;

import com.kasagichat.api.common.exception.BaseException;

/**
 * LLM認証情報が未設定の状態で生成機能を利用した場合の例外。
 */
public final class CredentialNotConfiguredException extends BaseException {

    public CredentialNotConfiguredException() {
        super(
            "CREDENTIAL_NOT_CONFIGURED",
            HttpStatus.CONFLICT,
            "先にAI接続設定を完了してください。"
        );
    }
}
