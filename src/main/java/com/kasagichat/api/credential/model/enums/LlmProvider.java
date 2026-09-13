package com.kasagichat.api.credential.model.enums;

/**
 * LLM呼び出しに使用するプロバイダー。
 */
public enum LlmProvider {
    /**
     * OpenAI。ユーザー自身のAPIキーを使用する。
     */
    OPENAI,

    /**
     * Anthropic。ユーザー自身のAPIキーを使用する。
     */
    ANTHROPIC,

    /**
     * デモ利用。合言葉で有効化し、運営のキーを回数制限付きで使用する。
     */
    DEMO
}
