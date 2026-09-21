package com.kasagichat.api.conversation.controller.dto.response;

import java.util.List;

/**
 * NPC誕生会話の振り返り結果。
 *
 * @param feedback ユーザーへ伝える肯定的なフィードバック
 * @param expGained 今回獲得したEXP
 * @param level 振り返り後のNPCレベル
 * @param leveledUp レベルアップしたか
 * @param newTopics 今回覚えた非公開の話題
 */
public record ReviewConversationResponse(
    String feedback,
    Integer expGained,
    Integer level,
    Boolean leveledUp,
    List<NewTopicResponse> newTopics
) {
}
