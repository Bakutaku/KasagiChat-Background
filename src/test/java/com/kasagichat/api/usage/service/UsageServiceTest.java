package com.kasagichat.api.usage.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kasagichat.api.conversation.model.enums.MessageRole;
import com.kasagichat.api.conversation.repository.ConversationRepository;
import com.kasagichat.api.conversation.repository.MessageRepository;
import com.kasagichat.api.credential.model.ApiCredential;
import com.kasagichat.api.credential.model.DemoUsage;
import com.kasagichat.api.credential.model.enums.LlmProvider;
import com.kasagichat.api.credential.repository.ApiCredentialRepository;
import com.kasagichat.api.credential.repository.DemoUsageRepository;
import com.kasagichat.api.event.repository.CardRepository;
import com.kasagichat.api.master.model.DemoPassphrase;
import com.kasagichat.api.usage.UsageProperties;
import com.kasagichat.api.usage.controller.dto.usage.response.UsageSummaryResponse;

@ExtendWith(MockitoExtension.class)
class UsageServiceTest {

    @Mock
    private MessageRepository messageRepository;
    @Mock
    private ConversationRepository conversationRepository;
    @Mock
    private CardRepository cardRepository;
    @Mock
    private ApiCredentialRepository apiCredentialRepository;
    @Mock
    private DemoUsageRepository demoUsageRepository;

    private UsageService service;

    @BeforeEach
    void setUp() {
        service = new UsageService(
                new UsageProperties(new BigDecimal("0.002")),
                messageRepository,
                conversationRepository,
                cardRepository,
                apiCredentialRepository,
                demoUsageRepository
        );
        when(messageRepository.countByConversationUserIdAndRole(1L, MessageRole.USER)).thenReturn(3L);
        when(conversationRepository.countByUserIdAndReviewedAtIsNotNull(1L)).thenReturn(1L);
        when(cardRepository.countByRecipientIdAndOpenedAtIsNotNull(1L)).thenReturn(2L);
    }

    @Test
    void estimatesCostFromLlmCalls() {
        when(apiCredentialRepository.findByUserId(1L))
                .thenReturn(Optional.of(ApiCredential.builder().provider(LlmProvider.OPENAI).build()));

        UsageSummaryResponse response = service.getSummary(1L);

        assertThat(response.provider()).isEqualTo(LlmProvider.OPENAI);
        assertThat(response.estimatedLlmCalls()).isEqualTo(6L);
        assertThat(response.estimatedCostUsd()).isEqualByComparingTo("0.012");
        assertThat(response.demo()).isNull();
    }

    @Test
    void returnsDemoRemainingCallsWithoutCost() {
        when(apiCredentialRepository.findByUserId(1L)).thenReturn(Optional.of(ApiCredential.builder()
                .provider(LlmProvider.DEMO)
                .demoPassphrase(DemoPassphrase.builder().passphrase("HOSHI26").callLimit(100).build())
                .build()));
        when(demoUsageRepository.findByUserId(1L)).thenReturn(Optional.of(DemoUsage.builder().callCount(30).build()));

        UsageSummaryResponse response = service.getSummary(1L);

        assertThat(response.estimatedCostUsd()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.demo().callCount()).isEqualTo(30);
        assertThat(response.demo().callLimit()).isEqualTo(100);
        assertThat(response.demo().remaining()).isEqualTo(70);
    }

    @Test
    void returnsNullProviderWithoutCredential() {
        when(apiCredentialRepository.findByUserId(1L)).thenReturn(Optional.empty());

        UsageSummaryResponse response = service.getSummary(1L);

        assertThat(response.provider()).isNull();
        assertThat(response.demo()).isNull();
    }
}
