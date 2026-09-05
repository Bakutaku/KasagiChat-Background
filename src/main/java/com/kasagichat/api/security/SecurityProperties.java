package com.kasagichat.api.security;

import java.net.URI;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import jakarta.validation.constraints.NotNull;

/**
 * application.ymlのapp.securityの設定値を保持するクラス
 * @param frontendUrl
 * @param jwt
 */
@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(
    @NotNull URI frontendUrl,
    @NotNull Duration pendingRegistrationTtl
) {
}
