package com.kasagichat.api.event.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.kasagichat.api.common.exception.BaseException;
import com.kasagichat.api.credential.service.LlmChatService;
import com.kasagichat.api.event.controller.dto.response.CardResponse;
import com.kasagichat.api.event.model.Card;
import com.kasagichat.api.event.model.Event;
import com.kasagichat.api.event.model.enums.VenueTemplate;
import com.kasagichat.api.event.repository.CardRepository;
import com.kasagichat.api.npc.model.Npc;
import com.kasagichat.api.npc.repository.NpcRepository;
import com.kasagichat.api.npc.repository.TopicRepository;
import com.kasagichat.api.security.model.Users;

/** DBやSpringコンテキストを起動しない、カードの参照と開封の単体テスト。 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CardServiceTest {

    @Mock private EventService eventService;
    @Mock private CardRepository cardRepository;
    @Mock private NpcRepository npcRepository;
    @Mock private TopicRepository topicRepository;
    @Mock private LlmChatService llmChatService;

    private CardService service;
    private Event event;
    private Card card;

    private final UUID cardId = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
    private final UUID eventId = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private final Users recipient = Users.builder().id(7L).displayName("受取人").build();
    private final Users partner = Users.builder().id(8L).displayName("相手").build();

    private static final String GENERATED = """
        <report>ハルさんの分身と、ゲームの話で盛り上がってきたよ。</report>
        <recommended_topics>
        最近遊んだゲームの話
        - おすすめのコーヒー豆の話
        </recommended_topics>
        """;

    @BeforeEach
    void setUp() {
        service = new CardService(
            eventService, cardRepository, npcRepository, topicRepository, llmChatService,
            new CardReportPromptFactory(), new CardReportParser());
        event = Event.builder().id(100L).publicId(eventId).creator(recipient).title("交流会")
            .startsAt(Instant.parse("2026-03-01T00:00:00Z"))
            .endsAt(Instant.parse("2099-03-02T00:00:00Z"))
            .venueTemplate(VenueTemplate.HALL).inviteCode("ABCD2345").build();
        card = Card.builder().id(500L).publicId(cardId).event(event)
            .recipient(recipient).partner(partner)
            .score(40).commonTags(new String[] {"ゲーム"}).build();
        when(cardRepository.findByPublicIdAndRecipientId(cardId, 7L)).thenReturn(Optional.of(card));
        when(npcRepository.findNameAndPresetsByUserIds(any()))
            .thenReturn(List.<Object[]>of(new Object[] {8L, "ハル", "cool-girl"}));
        when(npcRepository.findByUserId(7L)).thenReturn(Optional.of(
            Npc.builder().id(70L).user(recipient).name("ユウ").presetId("cheerful-girl")
                .profile("落ち着いた人").speechStyleEnabled(true).build()));
        when(npcRepository.findByUserId(8L)).thenReturn(Optional.of(
            Npc.builder().id(80L).user(partner).name("ハル").presetId("cool-girl")
                .profile("明るい人").speechStyleEnabled(true).build()));
        when(topicRepository.findByNpcIdAndPublicTopicIsTrueOrderByInterestDescIdAsc(anyLong()))
            .thenReturn(List.of());
    }

    @Test
    void hidesCardsThatBelongToSomeoneElseAsNotFound() {
        when(cardRepository.findByPublicIdAndRecipientId(cardId, 9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(9L, cardId))
            .isInstanceOfSatisfying(BaseException.class,
                exception -> assertThat(exception.getCode()).isEqualTo("CARD_NOT_FOUND"));
    }

    @Test
    void keepsTheReportHiddenUntilTheCardIsOpened() {
        card.setReport("読まれてはいけない報告");
        card.setRecommendedTopics(new String[] {"未開封のおすすめ"});

        CardResponse response = service.get(7L, cardId);

        assertThat(response.opened()).isFalse();
        assertThat(response.report()).isNull();
        assertThat(response.recommendedTopics()).isNull();
        assertThat(response.commonTags()).containsExactly("ゲーム");
        assertThat(response.partnerName()).isEqualTo("ハル");
        assertThat(response.partnerPresetId()).isEqualTo("cool-girl");
        assertThat(response.eventTitle()).isEqualTo("交流会");
    }

    @Test
    void generatesTheReportWithTheOpenersOwnCredential() {
        when(llmChatService.call(eq(7L), any())).thenReturn(GENERATED);

        CardResponse response = service.open(7L, cardId);

        assertThat(response.opened()).isTrue();
        assertThat(response.report()).isEqualTo("ハルさんの分身と、ゲームの話で盛り上がってきたよ。");
        assertThat(response.recommendedTopics())
            .containsExactly("最近遊んだゲームの話", "おすすめのコーヒー豆の話");
        assertThat(card.getOpenedAt()).isNotNull();
        verify(cardRepository).saveAndFlush(card);
    }

    @Test
    void limitsThePromptToPublicTopicsOfBothSides() {
        when(llmChatService.call(eq(7L), any())).thenReturn(GENERATED);

        service.open(7L, cardId);

        verify(topicRepository).findByNpcIdAndPublicTopicIsTrueOrderByInterestDescIdAsc(70L);
        verify(topicRepository).findByNpcIdAndPublicTopicIsTrueOrderByInterestDescIdAsc(80L);
        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        verify(llmChatService).call(eq(7L), prompt.capture());
        assertThat(prompt.getValue()).contains("ユウ", "ハル", "ゲーム");
    }

    @Test
    void returnsTheStoredReportWithoutCallingTheModelAgain() {
        card.setOpenedAt(Instant.parse("2026-03-01T10:00:00Z"));
        card.setReport("保存済みの報告");
        card.setRecommendedTopics(new String[] {"保存済みのおすすめ"});

        CardResponse response = service.open(7L, cardId);

        assertThat(response.report()).isEqualTo("保存済みの報告");
        assertThat(response.recommendedTopics()).containsExactly("保存済みのおすすめ");
        verifyNoInteractions(llmChatService);
        verify(cardRepository, never()).saveAndFlush(any());
    }

    @Test
    void refusesToOpenCardsOfAnEventThatHasNotStartedYet() {
        event.setStartsAt(Instant.parse("2099-01-01T00:00:00Z"));

        assertThatThrownBy(() -> service.open(7L, cardId))
            .isInstanceOfSatisfying(BaseException.class,
                exception -> assertThat(exception.getCode()).isEqualTo("EVENT_NOT_STARTED"));
        verifyNoInteractions(llmChatService);
    }

    @Test
    void leavesTheCardUnopenedWhenTheGeneratedTextHasNoReport() {
        when(llmChatService.call(eq(7L), any())).thenReturn("<recommended_topics>だけ</recommended_topics>");

        assertThatThrownBy(() -> service.open(7L, cardId))
            .isInstanceOfSatisfying(BaseException.class,
                exception -> assertThat(exception.getCode()).isEqualTo("LLM_CALL_FAILED"));
        assertThat(card.getOpenedAt()).isNull();
        verify(cardRepository, never()).saveAndFlush(any());
    }

    @Test
    void listsEventCardsOnlyForParticipants() {
        when(eventService.visibleEvent(7L, eventId)).thenReturn(event);
        when(cardRepository.findByEventIdAndRecipientIdOrderByScoreDesc(100L, 7L))
            .thenReturn(List.of(card));

        assertThat(service.listByEvent(7L, eventId)).extracting(CardResponse::partnerName)
            .containsExactly("ハル");
    }
}
