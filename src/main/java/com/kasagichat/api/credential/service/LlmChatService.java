package com.kasagichat.api.credential.service;

import java.time.Instant;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kasagichat.api.credential.crypto.ApiKeyCipher;
import com.kasagichat.api.credential.exception.CredentialNotConfiguredException;
import com.kasagichat.api.credential.exception.DemoLimitExceededException;
import com.kasagichat.api.credential.exception.LlmCallFailedException;
import com.kasagichat.api.credential.model.ApiCredential;
import com.kasagichat.api.credential.model.DemoUsage;
import com.kasagichat.api.credential.model.enums.LlmProvider;
import com.kasagichat.api.credential.provider.base.LlmProviderRegistry;
import com.kasagichat.api.credential.repository.ApiCredentialRepository;
import com.kasagichat.api.credential.repository.DemoUsageRepository;

import lombok.RequiredArgsConstructor;

/**
 * 保存済みの本人のAI接続設定を使って、秘密値を外へ出さずにLLMを呼び出す。
 */
@Service
@RequiredArgsConstructor
public class LlmChatService {

    private final ApiCredentialRepository apiCredentialRepository;
    private final DemoUsageRepository demoUsageRepository;
    private final LlmProviderRegistry providerRegistry;
    private final ApiKeyCipher apiKeyCipher;

    /**
     * ユーザーが設定したモデルを1回呼び出す。
     *
     * <p>設定行を排他ロックすることで、同一ユーザーのデモ利用回数が並列呼び出しで
     * 上限を超えないようにする。呼び出しまたは後続のDB更新が失敗した場合は、同じ
     * トランザクション内の利用回数更新もロールバックされる。</p>
     *
     * @param userId 認証済みユーザーの内部ID
     * @param prompt LLMへ渡すプロンプト
     * @return 空でない応答本文
     */
    @Transactional
    public String call(Long userId, String prompt) {
        ApiCredential credential = apiCredentialRepository.findByUserIdForUpdate(userId)
            .orElseThrow(CredentialNotConfiguredException::new);

        DemoUsage demoUsage = null;
        ChatModel chatModel;
        if (credential.getProvider() == LlmProvider.DEMO) {
            demoUsage = demoUsageRepository.findByUserId(userId)
                .orElseGet(() -> DemoUsage.builder().user(credential.getUser()).build());
            int used = demoUsage.getCallCount() == null ? 0 : demoUsage.getCallCount();
            int limit = Math.max(0, credential.getDemoPassphrase().getCallLimit());
            if (used >= limit) {
                throw new DemoLimitExceededException();
            }
            chatModel = providerRegistry.get(LlmProvider.DEMO).createChatModel(null, null);
        } else {
            String apiKey = apiKeyCipher.decrypt(credential.getEncryptedApiKey());
            chatModel = providerRegistry.get(credential.getProvider())
                .createChatModel(apiKey, credential.getModelName());
        }

        String response;
        try {
            response = chatModel.call(prompt);
        } catch (RuntimeException exception) {
            // プロバイダー例外に秘密値や入力本文が含まれる可能性があるため、引き継がない。
            throw new LlmCallFailedException();
        }
        if (response == null || response.isBlank()) {
            throw new LlmCallFailedException();
        }

        if (demoUsage != null) {
            int used = demoUsage.getCallCount() == null ? 0 : demoUsage.getCallCount();
            demoUsage.setCallCount(used + 1);
            demoUsage.setLastCalledAt(Instant.now());
            demoUsageRepository.save(demoUsage);
        }
        return response.strip();
    }
}
