package com.kasagichat.api.conversation.service;

import java.util.List;

/**
 * 振り返りのLLM出力を解析した結果。
 *
 * @param feedback ユーザーへ伝える肯定的なフィードバック
 * @param profile 人格文書
 * @param speechStyle 話し方の特徴
 * @param topics 会話で明示された話題
 * @param dailyQuestion 次回の今日のひとことで使う質問。生成されなかった場合はnull
 */
public record ConversationReview(
    String feedback,
    String profile,
    String speechStyle,
    List<GeneratedTopic> topics,
    String dailyQuestion
) {
}
