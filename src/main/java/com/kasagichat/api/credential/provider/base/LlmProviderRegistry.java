package com.kasagichat.api.credential.provider.base;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.kasagichat.api.credential.exception.LlmConfigurationException;
import com.kasagichat.api.credential.model.enums.LlmProvider;

/**
 * LLMプロバイダーと実装アダプターを対応付ける。
 */
@Component
public class LlmProviderRegistry {

    private final Map<LlmProvider, LlmProviderAdapter> adapters;

    public LlmProviderRegistry(List<LlmProviderAdapter> adapters) {
        EnumMap<LlmProvider, LlmProviderAdapter> byProvider = new EnumMap<>(LlmProvider.class);
        for (LlmProviderAdapter adapter : adapters) {
            if (byProvider.put(adapter.provider(), adapter) != null) {
                throw new IllegalStateException("LLM provider adapter is duplicated: " + adapter.provider());
            }
        }
        this.adapters = Map.copyOf(byProvider);
    }

    /**
     * 指定プロバイダーのアダプターを取得する。
     *
     * @param provider LLMプロバイダー
     * @return 対応するアダプター
     */
    public LlmProviderAdapter get(LlmProvider provider) {
        LlmProviderAdapter adapter = adapters.get(provider);
        if (adapter == null) {
            throw new LlmConfigurationException("指定されたLLMプロバイダーは利用できません");
        }
        return adapter;
    }
}
