package com.kasagichat.api.usage;

import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;

import jakarta.validation.constraints.NotNull;

/**
 * {@code application.yml}の{@code app.usage}設定値を保持する。
 *
 * @param estimatedCostPerCallUsd LLM呼び出し1回あたりの概算コスト（USD）
 */
@ConfigurationProperties(prefix = "app.usage")
public record UsageProperties(
    @NotNull BigDecimal estimatedCostPerCallUsd
) {
}
