package com.kasagichat.api.security;

import java.net.URI;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import jakarta.validation.constraints.NotNull;

/**
 * {@code application.yml}の{@code app.security}設定値を保持する。
 *
 * @param frontendUrl 認証完了後のリダイレクト先となるフロントエンドURL
 * @param pendingRegistrationTtl 仮登録情報の有効期間
 */
@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(
    @NotNull URI frontendUrl,
    @NotNull Duration pendingRegistrationTtl
) {
}
