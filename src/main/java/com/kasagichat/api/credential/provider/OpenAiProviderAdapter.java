package com.kasagichat.api.credential.provider;

import java.time.Duration;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Component;

import com.kasagichat.api.credential.model.enums.LlmProvider;
import com.kasagichat.api.credential.provider.base.LlmProviderAdapter;

/**
 * OpenAI向けSpring AIアダプター。
 */
@Component
public class OpenAiProviderAdapter implements LlmProviderAdapter {

    @Override
    public LlmProvider provider() {
        return LlmProvider.OPENAI;
    }

    @Override
    public ChatModel createChatModel(String apiKey, String model) {
        OpenAiChatOptions options = OpenAiChatOptions.builder()
            .apiKey(apiKey)
            .model(model)
            .maxTokens(1)
            .maxRetries(0)
            .timeout(Duration.ofSeconds(15))
            .build();
        return OpenAiChatModel.builder().options(options).build();
    }
}
