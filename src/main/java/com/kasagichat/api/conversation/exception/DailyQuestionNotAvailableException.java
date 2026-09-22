package com.kasagichat.api.conversation.exception;

import org.springframework.http.HttpStatus;

import com.kasagichat.api.common.exception.BaseException;

/**
 * 今日のひとことに使える未消化の質問が残っていない場合の例外。
 *
 * <p>質問は振り返りの成功時に生成されるため、まだ一度も振り返っていない場合に発生する。</p>
 */
public final class DailyQuestionNotAvailableException extends BaseException {

    public DailyQuestionNotAvailableException() {
        super(
            "DAILY_QUESTION_NOT_AVAILABLE",
            HttpStatus.CONFLICT,
            "今日のひとことに使える質問がまだ用意できていません。"
        );
    }
}
