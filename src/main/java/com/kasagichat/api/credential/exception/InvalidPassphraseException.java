package com.kasagichat.api.credential.exception;

import org.springframework.http.HttpStatus;

import com.kasagichat.api.common.exception.BaseException;

/**
 * デモ用合言葉が無効な場合の例外。
 */
public class InvalidPassphraseException extends BaseException {

    public InvalidPassphraseException() {
        super("INVALID_PASSPHRASE", HttpStatus.BAD_REQUEST, "デモ用の合言葉が無効です");
    }
}
