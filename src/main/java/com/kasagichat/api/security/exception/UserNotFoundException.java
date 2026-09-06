package com.kasagichat.api.security.exception;

import org.springframework.http.HttpStatus;

import com.kasagichat.api.common.exception.BaseException;

/**
 * 指定されたユーザーが存在しない場合に発生する例外。
 */
public final class UserNotFoundException extends BaseException {

    /**
     * 存在しないユーザーを表す例外を生成する。
     */
    public UserNotFoundException() {
        super(
                "USER_NOT_FOUND",
                HttpStatus.NOT_FOUND,
                "ユーザーが見つかりませんでした。"
        );
    }
}
