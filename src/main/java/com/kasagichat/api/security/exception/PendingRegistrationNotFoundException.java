package com.kasagichat.api.security.exception;

import org.springframework.http.HttpStatus;

import com.kasagichat.api.common.exception.BaseException;

/** セッションが参照する仮登録情報が存在しない場合の例外。 */
public final class PendingRegistrationNotFoundException extends BaseException {

    public PendingRegistrationNotFoundException() {
        super(
                "PENDING_REGISTRATION_NOT_FOUND",
                HttpStatus.UNAUTHORIZED,
                "仮登録情報が存在しません"
        );
    }
}
