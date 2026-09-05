package com.kasagichat.api.common.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;

/**
 * 独自例外ベース
 */
@Getter
public abstract class BaseException extends RuntimeException{

    private final String code;
    private final HttpStatus status;

    protected BaseException(String code, HttpStatus status, String message) {
        super(message);
        this.code = code;
        this.status = status;
    }
}
