package com.kasagichat.api.credential.provider.base;

import org.springframework.ai.chat.model.ChatModel;

import com.kasagichat.api.credential.model.enums.LlmProvider;

/**
 * プロバイダー固有のSpring AIモデル生成を共通化するアダプター。
 */
public interface LlmProviderAdapter {

    /**
     * このアダプターが扱うプロバイダーを返す。
     *
     * @return LLMプロバイダー
     */
    LlmProvider provider();

    /**
     * 指定されたキーとモデルを使うSpring AIのモデルを生成する。
     *
     * <p>base URLはユーザー入力から受け取らず、各SDKの固定接続先を使用する。</p>
     *
     * @param apiKey APIキー
     * @param model モデルID
     * @return Spring AI ChatModel
     */
    ChatModel createChatModel(String apiKey, String model);

    /**
     * キーとモデルが実際に利用可能か、1回の最小応答で確認する。
     *
     * @param apiKey APIキー
     * @param model モデルID
     */
    default void verify(String apiKey, String model) {
        createChatModel(apiKey, model).call("Reply with OK.");
    }
}
