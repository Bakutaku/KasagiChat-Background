package com.kasagichat.api.usage.controller.dto.usage.response;

import java.math.BigDecimal;

import com.kasagichat.api.credential.model.enums.LlmProvider;

/**
 * LLMの利用状況とコストの概算を返却する。
 *
 * @param provider 設定中のLLMプロバイダー。未設定の場合はnull
 * @param estimatedLlmCalls 保存済みのデータから数えたLLM呼び出し回数の概算
 * @param estimatedCostUsd コストの概算（USD）。デモ利用の場合は0
 * @param demo デモの利用回数。デモ利用でない場合はnull
 */
public record UsageSummaryResponse(
    LlmProvider provider,
    long estimatedLlmCalls,
    BigDecimal estimatedCostUsd,
    DemoUsageResponse demo
) {
}
