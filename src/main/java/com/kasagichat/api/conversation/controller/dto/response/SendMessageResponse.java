package com.kasagichat.api.conversation.controller.dto.response;

/**
 * 1往復成功後の状態。
 *
 * @param turn 成功後の往復数
 * @param reply NPCの応答
 * @param canFinish 振り返りを実行できるか
 * @param finished 上限に達して会話が終了したか
 */
public record SendMessageResponse(
    Integer turn,
    ConversationMessageResponse reply,
    boolean canFinish,
    boolean finished
) {
}
