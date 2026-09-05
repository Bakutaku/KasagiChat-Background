package com.kasagichat.api.security.exception;

import org.springframework.http.HttpStatus;

import com.kasagichat.api.common.exception.BaseException;

/** 仮登録情報の有効期限が切れている場合の例外。 */
public final class PendingRegistrationExpiredException extends BaseException {

    public PendingRegistrationExpiredException() {
        super(
                "PENDING_REGISTRATION_EXPIRED",
                HttpStatus.UNAUTHORIZED,
                "仮登録の有効期限が切れています"
        );
    }
}
