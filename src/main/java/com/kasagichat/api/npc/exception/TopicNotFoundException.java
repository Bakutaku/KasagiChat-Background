package com.kasagichat.api.npc.exception;

import org.springframework.http.HttpStatus;

import com.kasagichat.api.common.exception.BaseException;

/**
 * 話題が存在しない、または本人の話題ではない場合の例外。
 *
 * <p>他人の話題を操作した場合も同じエラーで返し、対象の存在を知られないようにする。
 * 家の品物（{@code HOME_ITEM_NOT_FOUND}）とはプロフィール帳という別画面で使うため、
 * クライアントが文言を出し分けられるよう専用のコードを持つ。</p>
 */
public final class TopicNotFoundException extends BaseException {

    public TopicNotFoundException() {
        super("TOPIC_NOT_FOUND", HttpStatus.NOT_FOUND, "話題が見つかりませんでした。");
    }
}
