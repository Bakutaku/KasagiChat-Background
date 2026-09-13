package com.kasagichat.api.npc.exception;

import org.springframework.http.HttpStatus;

import com.kasagichat.api.common.exception.BaseException;

/**
 * 指定された話題が存在しない、または他人の話題である場合に発生する例外。
 */
public final class TopicNotFoundException extends BaseException {

    /**
     * 存在しない話題を表す例外を生成する。
     */
    public TopicNotFoundException() {
        super(
                "TOPIC_NOT_FOUND",
                HttpStatus.NOT_FOUND,
                "話題が見つかりませんでした。"
        );
    }
}
