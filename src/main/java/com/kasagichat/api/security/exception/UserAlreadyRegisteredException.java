package com.kasagichat.api.security.exception;

import org.springframework.http.HttpStatus;

import com.kasagichat.api.common.exception.BaseException;

/** OAuthアカウントに対応するユーザーがすでに登録済みの場合の例外。 */
public final class UserAlreadyRegisteredException extends BaseException {

    public UserAlreadyRegisteredException() {
        super(
                "USER_ALREADY_REGISTERED",
                HttpStatus.CONFLICT,
                "すでに登録済みのユーザーです"
        );
    }
}
