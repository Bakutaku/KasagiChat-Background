package com.kasagichat.api.conversation.service;

import com.kasagichat.api.conversation.controller.dto.response.ConversationResponse;

/**
 * 会話開始APIが新規作成か再開かをHTTPステータスへ反映するための内部結果。
 *
 * @param conversation 会話状態
 * @param created 新規作成した場合はtrue
 */
public record ConversationStartResult(
    ConversationResponse conversation,
    boolean created
) {
}
