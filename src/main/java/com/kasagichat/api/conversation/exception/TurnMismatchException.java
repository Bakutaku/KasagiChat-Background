package com.kasagichat.api.conversation.exception;

import org.springframework.http.HttpStatus;

import com.kasagichat.api.common.exception.BaseException;

/**
 * クライアントが把握する往復数とサーバーの状態が一致しない場合の例外。
 */
public final class TurnMismatchException extends BaseException {

    public TurnMismatchException() {
        super(
            "TURN_MISMATCH",
            HttpStatus.CONFLICT,
            "会話の状態が更新されています。最新の会話を取得してください。"
        );
    }
}
