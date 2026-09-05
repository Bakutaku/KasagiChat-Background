package com.kasagichat.api.security.exception;

import com.kasagichat.api.common.exception.BaseException;

import lombok.Getter;

/**
 * TODO
 * 開発中に一時的に配置する例外
 * (後から適切な例外に置き換えるための一時記載用)
 */
@Getter
public final class TempException extends BaseException{
    
    public TempException() {
        super("TEMP","TODO 一時保管用");

    }
}
