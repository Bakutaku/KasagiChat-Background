package com.kasagichat.api.event.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.kasagichat.api.event.model.Card;
import com.kasagichat.api.event.model.Event;
import com.kasagichat.api.event.model.EventParticipant;
import com.kasagichat.api.event.model.enums.VenueTemplate;
import com.kasagichat.api.event.repository.CardRepository;
import com.kasagichat.api.event.repository.EventParticipantRepository;
import com.kasagichat.api.event.repository.EventRepository;
import com.kasagichat.api.master.model.TopicCategory;
import com.kasagichat.api.npc.model.Npc;
import com.kasagichat.api.npc.model.Topic;
import com.kasagichat.api.npc.repository.NpcRepository;
import com.kasagichat.api.npc.repository.TopicRepository;
import com.kasagichat.api.security.model.Users;

/** DBやSpringコンテキストを起動しない、相性計算とカードのupsertの単体テスト。 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CardMatchingServiceTest {

    @Mock private EventRepository eventRepository;
    @Mock private EventParticipantRepository eventParticipantRepository;
    @Mock private NpcRepository npcRepository;
    @Mock private TopicRepository topicRepository;
    @Mock private CardRepository cardRepository;

    private CardMatchingService service;
    private Event event;

    private final TopicCategory game = category(1L, "ゲーム");
    private final TopicCategory coffee = category(2L, "コーヒー");
    private final TopicCategory travel = category(3L, "旅行");

    @BeforeEach
    void setUp() {
        service = new CardMatchingService(
            eventRepository, eventParticipantRepository, npcRepository, topicRepository, cardRepository);
        event = Event.builder().id(100L).publicId(UUID.randomUUID())
            .creator(user(1L)).title("交流会")
            .startsAt(Instant.parse("2026-03-01T00:00:00Z"))
            .endsAt(Instant.parse("2026-03-02T00:00:00Z"))
            .venueTemplate(VenueTemplate.HALL).inviteCode("ABCD2345").build();
        when(eventRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(event));
        when(cardRepository.findByEventId(100L)).thenReturn(List.of());
    }

    @Test
    void doesNothingWhileTheEventHasOnlyOneParticipant() {
        stubParticipants(1L);

        service.recalculate(100L);

        verify(cardRepository, never()).saveAll(any());
    }

    @Test
    void scoresSharedCategoriesWeightedByBothInterests() {
        stubParticipants(1L, 2L);
        stubNpcs(1L, 2L);
        stubTopics(
            topic(11L, "ELDEN RING", game, 5),
            topic(21L, "スプラトゥーン", game, 4)
        );

        Map<String, Card> cards = saveAndIndex();

        // カテゴリ一致のみ: 2 * 5 * 4 = 40。話題名は重ならないためボーナスは付かない。
        assertThat(cards.get("1:2").getScore()).isEqualTo(40);
        assertThat(cards.get("1:2").getCommonTags()).containsExactly("ゲーム");
        // 相性は対称なので、両者に同じスコアのカードが届く。
        assertThat(cards.get("2:1").getScore()).isEqualTo(40);
    }

    @Test
    void addsBonusWhenTopicNamesOverlapInsideTheSameCategory() {
        stubParticipants(1L, 2L);
        stubNpcs(1L, 2L);
        stubTopics(
            topic(11L, "コーヒー", coffee, 3),
            topic(21L, "ハンドドリップのコーヒー", coffee, 3)
        );

        Map<String, Card> cards = saveAndIndex();

        // 2 * 3 * 3 = 18 に部分一致ボーナス5が加わる。
        assertThat(cards.get("1:2").getScore()).isEqualTo(23);
    }

    @Test
    void ignoresPrivateAndUncategorizedTopicsSoOnlySharedTagsRemain() {
        stubParticipants(1L, 2L);
        stubNpcs(1L, 2L);
        // Repositoryが公開・カテゴリ付きだけを返す前提なので、共通点なしの入力になる。
        stubTopics();

        Map<String, Card> cards = saveAndIndex();

        assertThat(cards.get("1:2").getScore()).isZero();
        assertThat(cards.get("1:2").getCommonTags()).isEmpty();
    }

    @Test
    void guaranteesAtLeastOneCardForEveryoneEvenWithoutAnySharedTag() {
        stubParticipants(1L, 2L, 3L, 4L, 5L);
        stubNpcs(1L, 2L, 3L, 4L, 5L);
        stubTopics();

        Map<String, Card> cards = saveAndIndex();

        for (long userId = 1L; userId <= 5L; userId++) {
            long recipient = userId;
            assertThat(cards.keySet()).anyMatch(key -> key.startsWith(recipient + ":"));
        }
    }

    @Test
    void deliversCardsBothWaysWhenOnlyOneSideRanksTheOtherInItsTopThree() {
        // 1は2〜5と共通点を持ち、5は1としか共通点が無い。1から見た5は4位だが、
        // 5から見た1は1位のため、相互保証で1にも5のカードが届く。
        stubParticipants(1L, 2L, 3L, 4L, 5L);
        stubNpcs(1L, 2L, 3L, 4L, 5L);
        stubTopics(
            topic(11L, "ゲームの話", game, 5),
            topic(12L, "コーヒーの話", coffee, 5),
            topic(13L, "旅行の話", travel, 5),
            topic(21L, "ゲーム", game, 5),
            topic(31L, "コーヒー", coffee, 5),
            topic(41L, "旅行", travel, 5),
            topic(51L, "ゲーム", game, 1)
        );

        Map<String, Card> cards = saveAndIndex();

        assertThat(cards).containsKey("1:5");
        assertThat(cards).containsKey("5:1");
        assertThat(cards.get("1:5").getScore()).isEqualTo(cards.get("5:1").getScore());
    }

    @Test
    void updatesOnlyScoreAndTagsSoAnOpenedReportSurvivesRecalculation() {
        stubParticipants(1L, 2L);
        stubNpcs(1L, 2L);
        stubTopics(
            topic(11L, "ELDEN RING", game, 5),
            topic(21L, "スプラトゥーン", game, 4)
        );
        Instant openedAt = Instant.parse("2026-03-01T10:00:00Z");
        Card opened = Card.builder()
            .id(500L).publicId(UUID.randomUUID()).event(event)
            .recipient(user(1L)).partner(user(2L))
            .score(1).commonTags(new String[] {"旅行"})
            .openedAt(openedAt).report("前の報告").recommendedTopics(new String[] {"前のおすすめ"})
            .build();
        when(cardRepository.findByEventId(100L)).thenReturn(new ArrayList<>(List.of(opened)));

        Map<String, Card> cards = saveAndIndex();

        Card updated = cards.get("1:2");
        assertThat(updated).isSameAs(opened);
        assertThat(updated.getScore()).isEqualTo(40);
        assertThat(updated.getCommonTags()).containsExactly("ゲーム");
        assertThat(updated.getOpenedAt()).isEqualTo(openedAt);
        assertThat(updated.getReport()).isEqualTo("前の報告");
        assertThat(updated.getRecommendedTopics()).containsExactly("前のおすすめ");
    }

    @Test
    void producesTheSameResultWhenRunTwiceWithTheSameParticipants() {
        stubParticipants(1L, 2L, 3L);
        stubNpcs(1L, 2L, 3L);
        stubTopics(
            topic(11L, "ゲーム", game, 5),
            topic(21L, "ゲーム", game, 5),
            topic(31L, "コーヒー", coffee, 5)
        );

        Map<String, Card> first = saveAndIndex();
        when(cardRepository.findByEventId(100L)).thenReturn(new ArrayList<>(first.values()));
        Map<String, Card> second = saveAndIndex();

        assertThat(second.keySet()).isEqualTo(first.keySet());
        second.forEach((key, card) -> assertThat(card.getScore()).isEqualTo(first.get(key).getScore()));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Card> saveAndIndex() {
        service.recalculate(100L);
        ArgumentCaptor<Iterable<Card>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(cardRepository, org.mockito.Mockito.atLeastOnce()).saveAll(captor.capture());
        List<Card> saved = new ArrayList<>();
        captor.getAllValues().get(captor.getAllValues().size() - 1).forEach(saved::add);
        return saved.stream().collect(Collectors.toMap(
            card -> card.getRecipient().getId() + ":" + card.getPartner().getId(),
            Function.identity()));
    }

    private void stubParticipants(Long... userIds) {
        List<EventParticipant> participants = new ArrayList<>();
        long id = 1L;
        for (Long userId : userIds) {
            participants.add(EventParticipant.builder()
                .id(id++).event(event).user(user(userId))
                .joinedAt(Instant.parse("2026-03-01T01:00:00Z")).build());
        }
        when(eventParticipantRepository.findByEventIdAndLeftAtIsNull(100L)).thenReturn(participants);
    }

    /** NPCの内部IDはユーザーIDの10倍にして、話題の所属を読みやすくする。 */
    private void stubNpcs(Long... userIds) {
        List<Npc> npcs = new ArrayList<>();
        for (Long userId : userIds) {
            npcs.add(npc(userId));
        }
        when(npcRepository.findByUserIdIn(any())).thenReturn(npcs);
    }

    private void stubTopics(Topic... topics) {
        when(topicRepository.findPublicCategorizedByNpcIds(anyCollection())).thenReturn(List.of(topics));
    }

    private Npc npc(Long userId) {
        return Npc.builder().id(userId * 10).user(user(userId))
            .name("分身" + userId).presetId("cheerful-girl").build();
    }

    /** 話題の内部IDの十の位をユーザーIDとして扱い、対応する分身へぶら下げる。 */
    private Topic topic(Long id, String name, TopicCategory topicCategory, int interest) {
        long userId = id / 10;
        return Topic.builder()
            .id(id).npc(npc(userId)).name(name).category(topicCategory)
            .interest((short) interest).publicTopic(true)
            .learnedAt(Instant.parse("2026-02-01T00:00:00Z")).build();
    }

    private TopicCategory category(Long id, String displayName) {
        return TopicCategory.builder()
            .id(id).code(displayName).name(displayName).displayName(displayName)
            .itemImagePath("/items/" + id + ".png").build();
    }

    private Users user(Long id) {
        return Users.builder().id(id).displayName("参加者" + id).build();
    }
}
