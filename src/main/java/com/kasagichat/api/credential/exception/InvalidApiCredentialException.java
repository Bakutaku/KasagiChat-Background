package com.kasagichat.api.credential.exception;

import org.springframework.http.HttpStatus;

import com.kasagichat.api.common.exception.BaseException;

/**
 * APIキーまたは選択モデルをプロバイダーで検証できなかった場合の例外。
 */
public class InvalidApiCredentialException extends BaseException {

    public InvalidApiCredentialException() {
        super("INVALID_API_KEY", HttpStatus.BAD_REQUEST, "APIキーまたはモデルを検証できませんでした");
    }
}
