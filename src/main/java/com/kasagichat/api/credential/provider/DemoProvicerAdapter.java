package com.kasagichat.api.credential.provider;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.kasagichat.api.credential.model.enums.LlmProvider;
import com.kasagichat.api.credential.provider.base.LlmProviderAdapter;

import lombok.RequiredArgsConstructor;

/**
 * デモ向けSpring AIアダプター。
 */
@Component 
@RequiredArgsConstructor 
public class DemoProvicerAdapter implements  LlmProviderAdapter {

    /**
     * デモ用のSpring AIモデル。ユーザー入力から受け取らず、固定のモデルを使用する。
     */
    private final @Qualifier("demoChatModel") ChatModel demoChatModel;
    
    @Override
    public LlmProvider provider() {
        return LlmProvider.DEMO;
    }

    @Override
    public ChatModel createChatModel(String apiKey, String model) {
        return demoChatModel;
    }
}
