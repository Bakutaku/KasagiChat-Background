package com.kasagichat.api.security.exception;

import org.springframework.http.HttpStatus;

import com.kasagichat.api.common.exception.BaseException;

/**
 * 仮登録情報の有効期限が切れている場合に発生する例外。
 */
public final class PendingRegistrationExpiredException extends BaseException {

    /**
     * 期限切れの仮登録情報を表す例外を生成する。
     */
    public PendingRegistrationExpiredException() {
        super(
                "PENDING_REGISTRATION_EXPIRED",
                HttpStatus.UNAUTHORIZED,
                "仮登録の有効期限が切れています"
        );
    }
}
