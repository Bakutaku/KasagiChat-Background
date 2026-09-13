package com.kasagichat.api.master.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kasagichat.api.conversation.model.enums.ConversationScene;
import com.kasagichat.api.conversation.model.enums.ConversationType;
import com.kasagichat.api.master.model.AchievementDef;
import com.kasagichat.api.master.model.ConversationOpening;
import com.kasagichat.api.master.model.DemoPassphrase;
import com.kasagichat.api.master.model.LevelCurve;
import com.kasagichat.api.master.repository.AchievementDefRepository;
import com.kasagichat.api.master.repository.ConversationOpeningRepository;
import com.kasagichat.api.master.repository.CounterDefRepository;
import com.kasagichat.api.master.repository.DemoPassphraseRepository;
import com.kasagichat.api.master.repository.ExpRuleRepository;
import com.kasagichat.api.master.repository.ItemRepository;
import com.kasagichat.api.master.repository.LevelCurveRepository;
import com.kasagichat.api.master.repository.TopicCategoryRepository;

@ExtendWith(MockitoExtension.class)
class DevelopmentMasterDataInitializerTest {

    @Mock
    private LevelCurveRepository levelCurveRepository;

    @Mock
    private ExpRuleRepository expRuleRepository;

    @Mock
    private TopicCategoryRepository topicCategoryRepository;

    @Mock
    private ConversationOpeningRepository conversationOpeningRepository;

    @Mock
    private DemoPassphraseRepository demoPassphraseRepository;

    @Mock
    private CounterDefRepository counterDefRepository;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private AchievementDefRepository achievementDefRepository;

    @Test
    void createsMasterDataOnlyWhenEmpty() {
        initializer().run(null);

        ArgumentCaptor<List<LevelCurve>> levelCurves = ArgumentCaptor.forClass(List.class);
        verify(levelCurveRepository).saveAll(levelCurves.capture());
        assertThat(levelCurves.getValue().getFirst().getLevel()).isEqualTo(1);
        assertThat(levelCurves.getValue().getFirst().getRequiredExp()).isZero();
        assertThat(levelCurves.getValue())
            .extracting(LevelCurve::getRequiredExp)
            .isSorted()
            .doesNotHaveDuplicates();

        ArgumentCaptor<List<ConversationOpening>> openings = ArgumentCaptor.forClass(List.class);
        verify(conversationOpeningRepository).saveAll(openings.capture());
        assertThat(openings.getValue())
            .extracting(ConversationOpening::getConversationType, ConversationOpening::getScene)
            .contains(
                Tuple.tuple(ConversationType.BIRTH, null),
                Tuple.tuple(ConversationType.PRACTICE, ConversationScene.CAFE),
                Tuple.tuple(ConversationType.PRACTICE, ConversationScene.LOBBY),
                Tuple.tuple(ConversationType.PRACTICE, ConversationScene.OFFICE)
            );

        ArgumentCaptor<DemoPassphrase> passphrase = ArgumentCaptor.forClass(DemoPassphrase.class);
        verify(demoPassphraseRepository).save(passphrase.capture());
        assertThat(passphrase.getValue().getPassphrase()).isEqualTo(DevelopmentMasterDataInitializer.DEMO_PASSPHRASE);
        assertThat(passphrase.getValue().getEnabled()).isTrue();

        ArgumentCaptor<List<AchievementDef>> achievementDefs = ArgumentCaptor.forClass(List.class);
        verify(achievementDefRepository).saveAll(achievementDefs.capture());
        AchievementDef firstCafe = achievementDefs.getValue().stream()
            .filter(def -> def.getCode().equals("FIRST_CAFE"))
            .findFirst()
            .orElseThrow();
        assertThat(firstCafe.getCounterDef().getCode()).isEqualTo("PRACTICE_CAFE");
        assertThat(firstCafe.getRewardItem().getCode()).isEqualTo("CASUAL_SHIRT");

        verify(expRuleRepository).saveAll(any());
        verify(topicCategoryRepository).saveAll(any());
        verify(counterDefRepository).saveAll(any());
        verify(itemRepository).saveAll(any());
    }

    @Test
    void doesNotCreateMasterDataWhenAlreadyExists() {
        when(levelCurveRepository.count()).thenReturn(1L);
        when(expRuleRepository.count()).thenReturn(1L);
        when(topicCategoryRepository.count()).thenReturn(1L);
        when(conversationOpeningRepository.count()).thenReturn(1L);
        when(demoPassphraseRepository.count()).thenReturn(1L);
        when(counterDefRepository.count()).thenReturn(1L);
        lenient().when(itemRepository.count()).thenReturn(1L);
        lenient().when(achievementDefRepository.count()).thenReturn(1L);

        initializer().run(null);

        verify(levelCurveRepository, never()).saveAll(any());
        verify(expRuleRepository, never()).saveAll(any());
        verify(topicCategoryRepository, never()).saveAll(any());
        verify(conversationOpeningRepository, never()).saveAll(any());
        verify(demoPassphraseRepository, never()).save(any());
        verify(counterDefRepository, never()).saveAll(any());
        verify(itemRepository, never()).saveAll(any());
        verify(achievementDefRepository, never()).saveAll(any());
    }

    private DevelopmentMasterDataInitializer initializer() {
        return new DevelopmentMasterDataInitializer(
            levelCurveRepository,
            expRuleRepository,
            topicCategoryRepository,
            conversationOpeningRepository,
            demoPassphraseRepository,
            counterDefRepository,
            itemRepository,
            achievementDefRepository
        );
    }
}
