package com.kasagichat.api.usage.controller.dto.usage.response;

/**
 * デモ利用のLLM呼び出し回数を返却する。
 *
 * @param callCount これまでのLLM呼び出し回数
 * @param callLimit 呼び出し回数の上限。合言葉が参照できない場合はnull
 * @param remaining 残りの呼び出し回数。上限が不明な場合はnull
 */
public record DemoUsageResponse(
    int callCount,
    Integer callLimit,
    Integer remaining
) {
}
