package com.kasagichat.api.usage.service;

import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kasagichat.api.conversation.model.enums.MessageRole;
import com.kasagichat.api.conversation.repository.ConversationRepository;
import com.kasagichat.api.conversation.repository.MessageRepository;
import com.kasagichat.api.credential.model.ApiCredential;
import com.kasagichat.api.credential.model.DemoUsage;
import com.kasagichat.api.credential.model.enums.LlmProvider;
import com.kasagichat.api.credential.repository.ApiCredentialRepository;
import com.kasagichat.api.credential.repository.DemoUsageRepository;
import com.kasagichat.api.event.repository.CardRepository;
import com.kasagichat.api.usage.UsageProperties;
import com.kasagichat.api.usage.controller.dto.usage.response.DemoUsageResponse;
import com.kasagichat.api.usage.controller.dto.usage.response.UsageSummaryResponse;

import lombok.RequiredArgsConstructor;

/**
 * LLMの利用状況とコストの概算を集計するService。
 *
 * <p>利用ログは保存していないため、LLMを呼び出す操作の結果として残るデータから回数を概算する。</p>
 */
@Service
@RequiredArgsConstructor
public class UsageService {

    private final UsageProperties usageProperties;

    private final MessageRepository messageRepository;

    private final ConversationRepository conversationRepository;

    private final CardRepository cardRepository;

    private final ApiCredentialRepository apiCredentialRepository;

    private final DemoUsageRepository demoUsageRepository;

    /**
     * ユーザーのLLM利用状況を集計する。
     *
     * @param userId ユーザーの内部ID
     * @return LLM呼び出し回数とコストの概算
     */
    @Transactional(readOnly = true)
    public UsageSummaryResponse getSummary(Long userId) {
        // 発言1回・振り返り1回・カード開封1回につき、LLMを1回呼び出す。
        long estimatedLlmCalls = messageRepository.countByConversationUserIdAndRole(userId, MessageRole.USER)
            + conversationRepository.countByUserIdAndReviewedAtIsNotNull(userId)
            + cardRepository.countByRecipientIdAndOpenedAtIsNotNull(userId);

        Optional<ApiCredential> credential = apiCredentialRepository.findByUserId(userId);
        LlmProvider provider = credential.map(ApiCredential::getProvider).orElse(null);
        if (provider == LlmProvider.DEMO) {
            // デモは運営のキーを使うため、ユーザーの負担は0とする。
            return new UsageSummaryResponse(provider, estimatedLlmCalls, BigDecimal.ZERO,
                toDemoUsage(userId, credential.get()));
        }

        BigDecimal estimatedCostUsd = usageProperties.estimatedCostPerCallUsd()
            .multiply(BigDecimal.valueOf(estimatedLlmCalls));
        return new UsageSummaryResponse(provider, estimatedLlmCalls, estimatedCostUsd, null);
    }

    private DemoUsageResponse toDemoUsage(Long userId, ApiCredential credential) {
        int callCount = demoUsageRepository.findByUserId(userId)
            .map(DemoUsage::getCallCount)
            .orElse(0);
        Integer callLimit = credential.getDemoPassphrase() == null
            ? null
            : credential.getDemoPassphrase().getCallLimit();
        Integer remaining = callLimit == null ? null : Math.max(0, callLimit - callCount);
        return new DemoUsageResponse(callCount, callLimit, remaining);
    }
}
