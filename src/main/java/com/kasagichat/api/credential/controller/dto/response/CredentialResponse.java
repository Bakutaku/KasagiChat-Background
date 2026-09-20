package com.kasagichat.api.credential.controller.dto.response;

import com.kasagichat.api.credential.model.enums.LlmProvider;

/**
 * 現在のLLM利用設定。生のAPIキーと合言葉は含めない。
 *
 * @param configured 設定済みか
 * @param provider 選択中のプロバイダー
 * @param model 選択中または運営固定のモデルID
 * @param maskedApiKey マスク済みAPIキー
 * @param demoUsage デモ利用回数
 */
public record CredentialResponse(
    boolean configured,
    LlmProvider provider,
    String model,
    String maskedApiKey,
    DemoUsageResponse demoUsage
) {

    public static CredentialResponse unconfigured() {
        return new CredentialResponse(false, null, null, null, null);
    }
}
