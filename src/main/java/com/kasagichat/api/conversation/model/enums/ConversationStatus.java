package com.kasagichat.api.conversation.model.enums;

/**
 * 会話の状態。
 */
public enum ConversationStatus {
    /**
     * 進行中。メッセージを送信できる。
     */
    IN_PROGRESS,

    /**
     * 終了済み。メッセージは送信できないが、まだ振り返っていない。
     */
    FINISHED,

    /**
     * 振り返り済み。
     */
    REVIEWED
}
