package com.kasagichat.api.credential.provider.config;

import java.time.Duration;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.kasagichat.api.credential.LlmProperties;
import com.kasagichat.api.credential.LlmProperties.DemoProperties;

/**
 * デバッグ用のデモ向けSpring AIモデルを提供する設定。
 */
@Configuration 
@Profile ("debug")
public class DebugDemollmConfiguration {
    

    @Bean("demoChatModel")
    ChatModel demoChatModel(LlmProperties properties) {
        DemoProperties demo = properties.demo();

        OpenAiChatOptions options = OpenAiChatOptions.builder()
            .baseUrl(demo.baseUrl())
            .apiKey(demo.apiKey())
            .model(demo.model())
            .maxTokens(1200)
            .maxRetries(0)
            .timeout(Duration.ofSeconds(45))
            .build();
        return OpenAiChatModel.builder().options(options).build();
    }
}
