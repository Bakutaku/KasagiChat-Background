package com.kasagichat.api.security.exception;

import org.springframework.http.HttpStatus;

import com.kasagichat.api.common.exception.BaseException;

/**
 * OAuthアカウントに対応するユーザーが登録済みの場合に発生する例外。
 */
public final class UserAlreadyRegisteredException extends BaseException {

    /**
     * 登録済みOAuthアカウントを表す例外を生成する。
     */
    public UserAlreadyRegisteredException() {
        super(
                "USER_ALREADY_REGISTERED",
                HttpStatus.CONFLICT,
                "すでに登録済みのユーザーです"
        );
    }
}
