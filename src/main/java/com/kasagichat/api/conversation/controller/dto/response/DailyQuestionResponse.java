package com.kasagichat.api.conversation.controller.dto.response;

/**
 * 今日のひとことで使う質問。
 *
 * <p>IDは返さない。会話の開始APIが未消化の最も古い質問を自分で選ぶため、
 * IDを渡せるようにすると選択経路が二重になる。</p>
 *
 * @param question 質問文
 */
public record DailyQuestionResponse(String question) {
}
