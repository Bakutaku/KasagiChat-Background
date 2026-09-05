package com.kasagichat.api.common.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;

/**
 * APIで扱うアプリケーション固有例外の基底クラス。
 */
@Getter
public abstract class BaseException extends RuntimeException{

    /**
     * クライアントがエラーを識別するためのコード。
     */
    private final String code;

    /**
     * レスポンスに使用するHTTPステータス。
     */
    private final HttpStatus status;

    /**
     * アプリケーション固有例外を生成する。
     *
     * @param code クライアント向けエラーコード
     * @param status レスポンスに使用するHTTPステータス
     * @param message エラーの説明
     */
    protected BaseException(String code, HttpStatus status, String message) {
        super(message);
        this.code = code;
        this.status = status;
    }
}
