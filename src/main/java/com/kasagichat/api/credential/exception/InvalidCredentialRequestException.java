package com.kasagichat.api.credential.exception;

import org.springframework.http.HttpStatus;

import com.kasagichat.api.common.exception.BaseException;

/**
 * プロバイダーと入力項目の組み合わせが不正な場合の例外。
 */
public class InvalidCredentialRequestException extends BaseException {

    public InvalidCredentialRequestException(String message) {
        super("INVALID_CREDENTIAL_REQUEST", HttpStatus.BAD_REQUEST, message);
    }
}
