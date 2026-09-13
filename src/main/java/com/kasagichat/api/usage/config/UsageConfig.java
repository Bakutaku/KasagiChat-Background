package com.kasagichat.api.usage.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.kasagichat.api.usage.UsageProperties;

/**
 * 利用状況の表示に使う設定値を有効にする。
 */
@Configuration
@EnableConfigurationProperties(UsageProperties.class)
public class UsageConfig {
}
