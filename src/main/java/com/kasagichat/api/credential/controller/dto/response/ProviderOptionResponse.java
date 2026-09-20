package com.kasagichat.api.credential.controller.dto.response;

import java.util.List;

import com.kasagichat.api.credential.model.enums.LlmProvider;

/**
 * フロントエンドが設定画面を構築するためのプロバイダー選択肢。
 *
 * @param provider プロバイダー
 * @param models 選択可能なモデル。DEMOは固定モデル1件
 * @param modelSelectable ユーザーがモデルを選択できるか
 * @param requiresApiKey APIキー入力が必要か
 * @param requiresPassphrase 合言葉入力が必要か
 * @param available 現在利用可能か
 */
public record ProviderOptionResponse(
    LlmProvider provider,
    List<String> models,
    boolean modelSelectable,
    boolean requiresApiKey,
    boolean requiresPassphrase,
    boolean available
) {
}
