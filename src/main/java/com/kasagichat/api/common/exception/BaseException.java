package com.kasagichat.api.common.exception;

import lombok.Getter;

/**
 * 独自例外ベース
 */
@Getter
public abstract class BaseException extends RuntimeException{

    private final String code;

    protected BaseException(String code, String message) {
        super(message);
        this.code = code;
    }
}
