package com.kasagichat.api.conversation.controller.dto.request;

/**
 * 会話一覧の絞り込み条件。
 *
 * <p>{@code UNREVIEWED}はIN_PROGRESSとFINISHEDの2状態にまたがるため、
 * 会話の状態を表すenumとは別に定義する。</p>
 */
public enum ConversationQueryStatus {
    /**
     * 振り返りがまだ済んでいない会話。
     */
    UNREVIEWED
}
