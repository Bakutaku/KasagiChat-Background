package com.kasagichat.api.db;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.hibernate.Hibernate;
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
import com.kasagichat.api.conversation.model.Message;
import com.kasagichat.api.conversation.model.enums.ConversationStatus;
import com.kasagichat.api.conversation.model.enums.ConversationType;
import com.kasagichat.api.conversation.model.enums.MessageRole;
import com.kasagichat.api.conversation.repository.ConversationRepository;
import com.kasagichat.api.conversation.repository.MessageRepository;
import com.kasagichat.api.master.model.AchievementDef;
import com.kasagichat.api.master.model.CounterDef;
import com.kasagichat.api.master.model.TopicCategory;
import com.kasagichat.api.master.model.enums.RewardType;
import com.kasagichat.api.npc.model.Achievement;
import com.kasagichat.api.npc.model.GrowthEvent;
import com.kasagichat.api.npc.model.enums.GrowthEventType;
import com.kasagichat.api.npc.repository.AchievementRepository;
import com.kasagichat.api.npc.repository.GrowthEventRepository;
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

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private AchievementRepository achievementRepository;

    @Autowired
    private GrowthEventRepository growthEventRepository;

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

    @Test
    void listsTopicsWithCategoryAndCountsThem() {
        TopicCategory category = entityManager.persist(TopicCategory.builder()
            .code("GAME")
            .name("ゲーム")
            .displayName("ゲーム機")
            .itemImagePath("/images/mementos/game.png")
            .build());
        Npc npc = npcRepository.save(npc(user("話題を一覧するユーザー")));
        Topic topic = topic(npc, "ゲーム実況", true);
        topic.setCategory(category);
        topicRepository.save(topic);
        flushAndClear();

        List<Topic> topics = topicRepository.findByNpcIdOrderByLearnedAtDesc(npc.getId());
        assertThat(topics).singleElement()
            .satisfies(found -> assertThat(Hibernate.isInitialized(found.getCategory())).isTrue());
        assertThat(topicRepository.countByNpcId(npc.getId())).isEqualTo(1L);
    }

    @Test
    void marksAchievementClaimedOnlyOnceByOwner() {
        Users owner = user("実績の持ち主");
        Users other = user("他人");
        CounterDef counter = entityManager.persist(CounterDef.builder().code("TEST_COUNTER").name("テスト").build());
        AchievementDef def = entityManager.persist(AchievementDef.builder()
            .code("TEST_ACHIEVEMENT")
            .name("テスト実績")
            .counterDef(counter)
            .threshold(1L)
            .rewardType(RewardType.NONE)
            .build());
        Achievement achievement = achievementRepository.save(Achievement.builder()
            .user(owner)
            .achievementDef(def)
            .achievedAt(Instant.now())
            .build());
        flushAndClear();

        Instant claimedAt = Instant.now();
        assertThat(achievementRepository.markClaimed(achievement.getId(), other.getId(), claimedAt)).isZero();
        assertThat(achievementRepository.markClaimed(achievement.getId(), owner.getId(), claimedAt)).isEqualTo(1);
        assertThat(achievementRepository.markClaimed(achievement.getId(), owner.getId(), claimedAt)).isZero();

        Achievement found = achievementRepository.findByIdAndUserId(achievement.getId(), owner.getId()).orElseThrow();
        assertThat(found.getClaimedAt()).isNotNull();
        assertThat(Hibernate.isInitialized(found.getAchievementDef())).isTrue();
        assertThat(achievementRepository.findByUserIdAndClaimedAtIsNullOrderByAchievedAtDesc(owner.getId())).isEmpty();
    }

    @Test
    void marksOnlyOwnUnreadGrowthEventsAsRead() {
        Users owner = user("通知の持ち主");
        Users other = user("他人");
        growthEventRepository.saveAll(List.of(
            growthEvent(owner, "1件目"),
            growthEvent(owner, "2件目"),
            growthEvent(other, "他人の通知")
        ));
        flushAndClear();

        assertThat(growthEventRepository.markAllAsRead(owner.getId(), Instant.now())).isEqualTo(2);
        assertThat(growthEventRepository.findByUserIdAndReadAtIsNullOrderByCreatedAtAsc(owner.getId())).isEmpty();
        assertThat(growthEventRepository.findByUserIdAndReadAtIsNullOrderByCreatedAtAsc(other.getId())).hasSize(1);
        assertThat(growthEventRepository.findTop50ByUserIdOrderByCreatedAtDesc(owner.getId())).hasSize(2);
    }

    @Test
    void countsUsageSources() {
        Users user = user("利用状況を見るユーザー");
        Conversation conversation = conversationRepository.save(Conversation.builder()
            .user(user)
            .type(ConversationType.DAILY)
            .status(ConversationStatus.REVIEWED)
            .reviewedAt(Instant.now())
            .build());
        messageRepository.saveAll(List.of(
            message(conversation, 1, MessageRole.ASSISTANT),
            message(conversation, 2, MessageRole.USER),
            message(conversation, 3, MessageRole.ASSISTANT)
        ));
        flushAndClear();

        assertThat(messageRepository.countByConversationUserIdAndRole(user.getId(), MessageRole.USER)).isEqualTo(1L);
        assertThat(conversationRepository.countByUserIdAndReviewedAtIsNotNull(user.getId())).isEqualTo(1L);
        assertThat(cardRepository.countByRecipientIdAndOpenedAtIsNotNull(user.getId())).isZero();
    }

    private GrowthEvent growthEvent(Users user, String message) {
        return GrowthEvent.builder().user(user).type(GrowthEventType.LEVEL_UP).message(message).build();
    }

    private Message message(Conversation conversation, int seq, MessageRole role) {
        return Message.builder().conversation(conversation).seq(seq).role(role).text("メッセージ" + seq).build();
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
