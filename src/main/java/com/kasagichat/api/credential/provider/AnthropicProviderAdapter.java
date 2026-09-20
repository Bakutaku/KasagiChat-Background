package com.kasagichat.api.credential.provider;

import java.time.Duration;

import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

import com.kasagichat.api.credential.model.enums.LlmProvider;
import com.kasagichat.api.credential.provider.base.LlmProviderAdapter;

/**
 * Anthropic向けSpring AIアダプター。
 */
@Component
public class AnthropicProviderAdapter implements LlmProviderAdapter {

    @Override
    public LlmProvider provider() {
        return LlmProvider.ANTHROPIC;
    }

    @Override
    public ChatModel createChatModel(String apiKey, String model) {
        AnthropicChatOptions options = AnthropicChatOptions.builder()
            .apiKey(apiKey)
            .model(model)
            .maxTokens(1)
            .maxRetries(0)
            .timeout(Duration.ofSeconds(15))
            .build();
        return AnthropicChatModel.builder().options(options).build();
    }
}
