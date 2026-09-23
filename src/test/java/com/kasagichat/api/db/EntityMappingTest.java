package com.kasagichat.api.db;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.kasagichat.api.conversation.model.Conversation;
import com.kasagichat.api.conversation.model.enums.ConversationStatus;
import com.kasagichat.api.conversation.model.enums.ConversationType;
import com.kasagichat.api.conversation.repository.ConversationRepository;
import com.kasagichat.api.event.model.Card;
import com.kasagichat.api.event.model.Event;
import com.kasagichat.api.event.model.enums.VenueTemplate;
import com.kasagichat.api.event.repository.CardRepository;
import com.kasagichat.api.event.repository.EventRepository;
import com.kasagichat.api.npc.model.Memory;
import com.kasagichat.api.npc.model.Npc;
import com.kasagichat.api.npc.model.Topic;
import com.kasagichat.api.npc.repository.MemoryRepository;
import com.kasagichat.api.npc.repository.NpcRepository;
import com.kasagichat.api.npc.repository.TopicRepository;
import com.kasagichat.api.security.model.Users;
import com.kasagichat.api.security.repository.UsersRepository;

/**
 * 実際のPostgreSQLで、エンティティのマッピングと主要なRepositoryメソッドを確認する。
 *
 * <p>Dockerが使えない環境（Dev Container内など）ではスキップされる。</p>
 */
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("debug")
@Testcontainers(disabledWithoutDocker = true)
class EntityMappingTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private NpcRepository npcRepository;

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private MemoryRepository memoryRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private CardRepository cardRepository;

    @Test
    void savesNpcAppearanceAsJsonb() {
        Users user = user("NPCの持ち主");
        Npc npc = npcRepository.save(npc(user));
        flushAndClear();

        Npc found = npcRepository.findByUserId(user.getId()).orElseThrow();
        assertThat(found.getId()).isEqualTo(npc.getId());
        assertThat(found.getAppearance()).containsEntry("hair", "hair_01");
        assertThat(found.getLevel()).isEqualTo(1);
        assertThat(found.getBornAt()).isNull();
    }

    @Test
    void savesCardTagsAsArray() {
        Users recipient = user("受取人");
        Users partner = user("相手");
        Event event = eventRepository.save(Event.builder()
            .creator(recipient)
            .title("テストイベント")
            .startsAt(Instant.parse("2026-09-01T00:00:00Z"))
            .endsAt(Instant.parse("2026-09-30T00:00:00Z"))
            .venueTemplate(VenueTemplate.HALL)
            .inviteCode("TEST01")
            .build());
        Card card = cardRepository.save(Card.builder()
            .event(event)
            .recipient(recipient)
            .partner(partner)
            .score(12)
            .commonTags(new String[] {"ゲーム", "カフェ・飲み物"})
            .build());
        flushAndClear();

        Card found = cardRepository.findByPublicIdAndRecipientId(card.getPublicId(), recipient.getId()).orElseThrow();
        assertThat(found.getCommonTags()).containsExactly("ゲーム", "カフェ・飲み物");
        assertThat(found.getRecommendedTopics()).isNull();
        assertThat(cardRepository.findByPublicIdAndRecipientId(card.getPublicId(), partner.getId())).isEmpty();
    }

    @Test
    void conversationIsOnlyVisibleToOwner() {
        Users owner = user("会話の持ち主");
        Users other = user("他人");
        Conversation conversation = conversationRepository.save(Conversation.builder()
            .user(owner)
            .type(ConversationType.DAILY)
            .build());
        flushAndClear();

        assertThat(conversation.getPublicId()).isNotNull();
        assertThat(conversationRepository.findByPublicIdAndUserId(conversation.getPublicId(), owner.getId())).isPresent();
        assertThat(conversationRepository.findByPublicIdAndUserId(conversation.getPublicId(), other.getId())).isEmpty();
    }

    @Test
    void findsResumableConversationWithoutScene() {
        Users user = user("再開するユーザー");
        conversationRepository.save(Conversation.builder()
            .user(user)
            .type(ConversationType.DAILY)
            .status(ConversationStatus.REVIEWED)
            .build());
        Conversation inProgress = conversationRepository.save(Conversation.builder()
            .user(user)
            .type(ConversationType.DAILY)
            .build());
        flushAndClear();

        Conversation found = conversationRepository
            .findFirstByUserIdAndTypeAndSceneAndStatusNotOrderByCreatedAtDesc(
                user.getId(), ConversationType.DAILY, null, ConversationStatus.REVIEWED)
            .orElseThrow();
        assertThat(found.getId()).isEqualTo(inProgress.getId());
    }

    @Test
    void deletingTopicRemovesItsMemories() {
        Npc npc = npcRepository.save(npc(user("話題を消すユーザー")));
        Topic topic = topicRepository.save(topic(npc, "映画", true));
        memoryRepository.save(Memory.builder().topic(topic).content("最近映画を観た").build());
        flushAndClear();

        topicRepository.delete(topicRepository.findById(topic.getId()).orElseThrow());
        flushAndClear();

        assertThat(topicRepository.count()).isZero();
        assertThat(memoryRepository.count()).isZero();
    }

    @Test
    void deletingAPlacedTopicAlsoFreesItsHomeSlot() {
        Npc npc = npcRepository.save(npc(user("配置した話題を消すユーザー")));
        Topic placed = topicRepository.save(topic(npc, "映画", true));
        placed.setHomeSlotId("BOOKSHELF_1");
        topicRepository.save(placed);
        memoryRepository.save(Memory.builder().topic(placed).content("映画館へ行った").build());
        flushAndClear();

        topicRepository.delete(topicRepository.findByIdAndNpcUserId(placed.getId(), npc.getUser().getId())
            .orElseThrow());
        flushAndClear();

        assertThat(topicRepository.findByNpcIdOrderByLearnedAtDescIdDesc(npc.getId())).isEmpty();
        assertThat(memoryRepository.count()).isZero();
        // 同じスロットを別の話題で再利用できる（配置の一意制約が残っていない）。
        Topic replacement = topicRepository.save(topic(npc, "読書", false));
        replacement.setHomeSlotId("BOOKSHELF_1");
        topicRepository.save(replacement);
        flushAndClear();
        assertThat(topicRepository.findById(replacement.getId()).orElseThrow().getHomeSlotId())
            .isEqualTo("BOOKSHELF_1");
    }

    @Test
    void findsTopicsOnlyForTheirOwner() {
        Npc owner = npcRepository.save(npc(user("話題の持ち主")));
        Npc other = npcRepository.save(npc(user("他人")));
        Topic topic = topicRepository.save(topic(owner, "ゲーム", false));
        flushAndClear();

        assertThat(topicRepository.findByIdAndNpcUserId(topic.getId(), owner.getUser().getId())).isPresent();
        assertThat(topicRepository.findByIdAndNpcUserId(topic.getId(), other.getUser().getId())).isEmpty();
    }

    @Test
    void readsOnlyMemoriesOfPublicTopicsForCards() {
        Npc npc = npcRepository.save(npc(user("思い出のあるユーザー")));
        Topic publicTopic = topicRepository.save(topic(npc, "ゲーム", true));
        Topic privateTopic = topicRepository.save(topic(npc, "家族のこと", false));
        memoryRepository.save(Memory.builder().topic(publicTopic).content("新作ゲームを遊んでいる").build());
        memoryRepository.save(Memory.builder().topic(privateTopic).content("家族の相談をした").build());
        flushAndClear();

        List<Memory> memories = memoryRepository.findByTopicIdInAndTopicPublicTopicTrue(
            List.of(publicTopic.getId(), privateTopic.getId()));
        assertThat(memories).extracting(Memory::getContent).containsExactly("新作ゲームを遊んでいる");
    }

    private Users user(String displayName) {
        return usersRepository.save(Users.builder().displayName(displayName).build());
    }

    private Npc npc(Users user) {
        return Npc.builder()
            .user(user)
            .name("テスト")
            .presetId("SAMPLE_A")
            .appearance(Map.of("body", "body_01", "hair", "hair_01"))
            .build();
    }

    private Topic topic(Npc npc, String name, boolean publicTopic) {
        return Topic.builder()
            .npc(npc)
            .name(name)
            .interest((short) 3)
            .publicTopic(publicTopic)
            .learnedAt(Instant.now())
            .build();
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
