package com.kasagichat.api.security.exception;

import org.springframework.http.HttpStatus;

import com.kasagichat.api.common.exception.BaseException;

/**
 * セッションが参照する仮登録情報が存在しない場合に発生する例外。
 */
public final class PendingRegistrationNotFoundException extends BaseException {

    /**
     * 存在しない仮登録情報を表す例外を生成する。
     */
    public PendingRegistrationNotFoundException() {
        super(
                "PENDING_REGISTRATION_NOT_FOUND",
                HttpStatus.UNAUTHORIZED,
                "仮登録情報が存在しません"
        );
    }
}
