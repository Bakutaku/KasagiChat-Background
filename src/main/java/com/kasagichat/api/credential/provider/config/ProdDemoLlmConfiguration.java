package com.kasagichat.api.credential.provider.config;

import org.springframework.ai.bedrock.converse.BedrockProxyChatModel;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * デモ用のLLMモデルを提供するSpring Bean定義。
 * 本番環境では、AWS Bedrockのプロキシモデルを利用する。
 */
@Configuration 
@Profile ("prod")
class ProdDemoLlmConfiguration {

    @Bean ("demoChatModel")
    ChatModel demoChatModel(BedrockProxyChatModel chatModel) {
        return chatModel;
    }
}