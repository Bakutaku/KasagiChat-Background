package com.kasagichat.api.credential.controller.dto.request;

import com.kasagichat.api.credential.model.enums.LlmProvider;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * APIキーまたはデモ利用設定の登録・変更リクエスト。
 *
 * <p>OPENAI/ANTHROPICはmodelとapiKey、DEMOはpassphraseだけを受け付ける。</p>
 *
 * @param provider 選択するプロバイダー
 * @param model BYOKで選択するモデルID
 * @param apiKey BYOKのAPIキー
 * @param passphrase デモ用合言葉
 */
public record CredentialUpdateRequest(
    @NotNull LlmProvider provider,
    @Size(max = 100) String model,
    @Size(max = 512) String apiKey,
    @Size(max = 50) String passphrase
) {
}
