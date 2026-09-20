package com.kasagichat.api.credential.controller.dto.response;

import java.util.List;

/**
 * APIキー設定画面で利用できるプロバイダーとモデル。
 *
 * @param providers プロバイダー選択肢
 */
public record CredentialOptionsResponse(List<ProviderOptionResponse> providers) {
}
