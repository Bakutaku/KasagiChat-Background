package com.kasagichat.api.credential.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.kasagichat.api.credential.LlmProperties;

/**
 * LLM関連の型安全な設定値を有効化する。
 */
@Configuration
@EnableConfigurationProperties(LlmProperties.class)
public class LlmConfiguration {
}
