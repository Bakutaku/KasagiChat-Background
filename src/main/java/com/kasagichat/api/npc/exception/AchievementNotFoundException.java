package com.kasagichat.api.npc.exception;

import org.springframework.http.HttpStatus;

import com.kasagichat.api.common.exception.BaseException;

/**
 * 指定された実績が存在しない、または他人の実績である場合に発生する例外。
 */
public final class AchievementNotFoundException extends BaseException {

    /**
     * 存在しない実績を表す例外を生成する。
     */
    public AchievementNotFoundException() {
        super(
                "ACHIEVEMENT_NOT_FOUND",
                HttpStatus.NOT_FOUND,
                "実績が見つかりませんでした。"
        );
    }
}
