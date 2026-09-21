package com.kasagichat.api.npc.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kasagichat.api.conversation.model.Conversation;
import com.kasagichat.api.master.model.Item;
import com.kasagichat.api.master.model.LevelCurve;
import com.kasagichat.api.master.model.TopicCategory;
import com.kasagichat.api.master.model.enums.ItemType;
import com.kasagichat.api.master.repository.LevelCurveRepository;
import com.kasagichat.api.npc.exception.HomeException;
import com.kasagichat.api.npc.model.Npc;
import com.kasagichat.api.npc.model.Topic;
import com.kasagichat.api.npc.model.UnlockedItem;
import com.kasagichat.api.npc.model.enums.HomeItemKind;
import com.kasagichat.api.npc.repository.NpcRepository;
import com.kasagichat.api.npc.repository.TopicRepository;
import com.kasagichat.api.npc.repository.UnlockedItemRepository;
import com.kasagichat.api.security.exception.UserNotFoundException;
import com.kasagichat.api.security.model.Users;
import com.kasagichat.api.security.service.UserService;

/** DBやSpringコンテキストを起動しない、所有権・配置・返却契約の単体テスト。 */
@ExtendWith(MockitoExtension.class)
class HomeServiceTest {
    @Mock private UserService userService;
    @Mock private NpcRepository npcRepository;
    @Mock private TopicRepository topicRepository;
    @Mock private UnlockedItemRepository unlockedItemRepository;
    @Mock private LevelCurveRepository levelCurveRepository;

    private HomeService service;
    private Users user;
    private Npc npc;
    private Topic topic;
    private final Instant learnedAt = Instant.parse("2026-09-20T00:00:00Z");

    @BeforeEach
    void setUp() {
        service = new HomeService(userService, npcRepository, topicRepository, unlockedItemRepository, levelCurveRepository);
        user = Users.builder().id(7L).displayName("本人").build();
        npc = Npc.builder().id(11L).user(user).name("分身").presetId("SAMPLE_A").level(2).exp(35).build();
        topic = Topic.builder().id(21L).npc(npc).name("最近読んだ本").interest((short) 3)
            .learnedAt(learnedAt).category(category()).build();
    }

    @Test
    void returnsOwnedInventoryAndSafeLevelProgressWithoutWriting() {
        stubHome();
        UUID conversationId = UUID.randomUUID();
        topic.setSourceConversation(Conversation.builder().user(user).publicId(conversationId).build());
        topic.setHomeSlotId("DISPLAY_2");
        when(topicRepository.findByNpcIdOrderByLearnedAtDescIdDesc(11L)).thenReturn(List.of(topic));
        when(unlockedItemRepository.findByUserIdOrderByUnlockedAtDescIdDesc(7L)).thenReturn(List.of(
            UnlockedItem.builder().id(31L).user(user).unlockedAt(learnedAt)
                .item(Item.builder().code("BERET").itemType(ItemType.ACCESSORY).name("帽子")
                    .imagePath("/images/beret.png").build()).build()
        ));
        when(levelCurveRepository.findById(2)).thenReturn(Optional.of(curve(2, 20)));
        when(levelCurveRepository.findById(3)).thenReturn(Optional.of(curve(3, 50)));

        var home = service.getHome(7L);

        assertThat(home.npc().name()).isEqualTo("分身");
        assertThat(home.npc().level()).isEqualTo(2);
        assertThat(home.npc().exp()).isEqualTo(35);
        assertThat(home.npc().nextLevel().requiredTotalExp()).isEqualTo(50);
        assertThat(home.npc().nextLevel().remainingExp()).isEqualTo(15);
        assertThat(home.slots()).hasSize(12);
        var item = home.items().getFirst();
        assertThat(item.topicId()).isEqualTo(21L);
        assertThat(item.category().code()).isEqualTo("READING");
        assertThat(item.kind()).isEqualTo(HomeItemKind.SOUVENIR);
        assertThat(item.acquiredAt()).isEqualTo(learnedAt);
        assertThat(item.sourceConversationId()).isEqualTo(conversationId);
        assertThat(item.publicTopic()).isFalse();
        assertThat(item.slotId()).isEqualTo("DISPLAY_2");
        assertThat(home.unlockedItems().getFirst().itemType()).isEqualTo(ItemType.ACCESSORY);
        verify(topicRepository, never()).saveAndFlush(any());
    }

    @Test
    void derivesUncategorizedBookWithoutInventingAnAssetOrLeakingForeignConversation() {
        stubHome();
        topic.setCategory(null);
        topic.setSourceConversation(Conversation.builder()
            .user(Users.builder().id(999L).build()).publicId(UUID.randomUUID()).build());
        when(topicRepository.findByNpcIdOrderByLearnedAtDescIdDesc(11L)).thenReturn(List.of(topic));

        var item = service.getHome(7L).items().getFirst();

        assertThat(item.kind()).isEqualTo(HomeItemKind.BOOK);
        assertThat(item.displayName()).isEqualTo(topic.getName());
        assertThat(item.imagePath()).isNull();
        assertThat(item.category()).isNull();
        assertThat(item.sourceConversationId()).isNull();
        assertThat(item.slotId()).isNull();
    }

    @Test
    void returnsEmptyInventoryAndUnknownNextLevelWhenNoDataExists() {
        stubHome();
        var home = service.getHome(7L);
        assertThat(home.items()).isEmpty();
        assertThat(home.unlockedItems()).isEmpty();
        assertThat(home.npc().nextLevel()).isNull();
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 20, 35})
    void doesNotInventProgressFromNonIncreasingOrAlreadyReachedThreshold(int nextExp) {
        stubHome();
        when(levelCurveRepository.findById(2)).thenReturn(Optional.of(curve(2, 20)));
        when(levelCurveRepository.findById(3)).thenReturn(Optional.of(curve(3, nextExp)));
        assertThat(service.getHome(7L).npc().nextLevel()).isNull();
    }

    @Test
    void doesNotInferNextLevelAcrossMissingMasterRows() {
        stubHome();
        when(levelCurveRepository.findById(2)).thenReturn(Optional.of(curve(2, 20)));
        assertThat(service.getHome(7L).npc().nextLevel()).isNull();
    }

    @Test
    void doesNotOverflowNextLevelNumber() {
        stubHome();
        npc.setLevel(Integer.MAX_VALUE);
        assertThat(service.getHome(7L).npc().nextLevel()).isNull();
        verifyNoInteractions(levelCurveRepository);
    }

    @Test
    void doesNotReturnProgressWhenCurrentLevelThresholdExceedsExp() {
        stubHome();
        when(levelCurveRepository.findById(2)).thenReturn(Optional.of(curve(2, 40)));
        when(levelCurveRepository.findById(3)).thenReturn(Optional.of(curve(3, 50)));
        assertThat(service.getHome(7L).npc().nextLevel()).isNull();
    }

    @Test
    void rejectsDeletedUserBeforeReadingNpc() {
        when(userService.getCurrentUser(7L)).thenThrow(new UserNotFoundException());
        assertThatThrownBy(() -> service.getHome(7L)).isInstanceOf(UserNotFoundException.class);
        verifyNoInteractions(npcRepository, topicRepository, unlockedItemRepository);
    }

    @Test
    void rejectsMissingNpcOnReadAndWrite() {
        assertCode(() -> service.getHome(7L), "NPC_NOT_FOUND");
        assertCode(() -> service.placeItem(7L, 21L, "DISPLAY_1"), "NPC_NOT_FOUND");
        verifyNoInteractions(topicRepository, unlockedItemRepository);
    }

    @Test
    void acquiresOwnerLockBeforeCheckingAndSavingPlacement() {
        stubOwnedTopic();
        var result = service.placeItem(7L, 21L, "DISPLAY_1");
        assertThat(result.slotId()).isEqualTo("DISPLAY_1");
        var order = inOrder(userService, npcRepository, topicRepository);
        order.verify(userService).getCurrentUser(7L);
        order.verify(npcRepository).findByUserIdForUpdate(7L);
        order.verify(topicRepository).findByIdAndNpcId(21L, 11L);
        order.verify(topicRepository).existsByNpcIdAndHomeSlotIdAndIdNot(11L, "DISPLAY_1", 21L);
        order.verify(topicRepository).saveAndFlush(topic);
    }

    @Test
    void movesWithoutChangingAcquisitionOrTopicData() {
        stubOwnedTopic();
        topic.setHomeSlotId("DISPLAY_1");
        service.placeItem(7L, 21L, "DISPLAY_2");
        assertThat(topic.getHomeSlotId()).isEqualTo("DISPLAY_2");
        assertThat(topic.getLearnedAt()).isEqualTo(learnedAt);
        assertThat(topic.getName()).isEqualTo("最近読んだ本");
    }

    @Test
    void sameSlotIsIdempotent() {
        stubOwnedTopic();
        topic.setHomeSlotId("DISPLAY_1");
        assertThat(service.placeItem(7L, 21L, "DISPLAY_1").slotId()).isEqualTo("DISPLAY_1");
        verify(topicRepository, never()).saveAndFlush(any());
    }

    @Test
    void occupiedDestinationKeepsOriginalPlacement() {
        stubOwnedTopic();
        topic.setHomeSlotId("DISPLAY_1");
        when(topicRepository.existsByNpcIdAndHomeSlotIdAndIdNot(11L, "DISPLAY_2", 21L)).thenReturn(true);
        assertCode(() -> service.placeItem(7L, 21L, "DISPLAY_2"), "HOME_SLOT_OCCUPIED");
        assertThat(topic.getHomeSlotId()).isEqualTo("DISPLAY_1");
        verify(topicRepository, never()).saveAndFlush(any());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"DISPLAY_7", "display_1", "DISPLAY_1 ", "floor-1", " "})
    void rejectsForgedSlot(String slotId) {
        stubOwnedTopic();
        assertCode(() -> service.placeItem(7L, 21L, slotId), "INVALID_HOME_SLOT");
        verify(topicRepository, never()).saveAndFlush(any());
    }

    @Test
    void validatesCompatibilityInBothDirections() {
        stubOwnedTopic();
        assertCode(() -> service.placeItem(7L, 21L, "BOOKSHELF_1"), "HOME_SLOT_INCOMPATIBLE");
        topic.setCategory(null);
        assertCode(() -> service.placeItem(7L, 21L, "DISPLAY_1"), "HOME_SLOT_INCOMPATIBLE");
        assertThat(service.placeItem(7L, 21L, "BOOKSHELF_1").slotId()).isEqualTo("BOOKSHELF_1");
    }

    @Test
    void missingOrForeignItemCannotBePlacedOrStored() {
        when(npcRepository.findByUserIdForUpdate(7L)).thenReturn(Optional.of(npc));
        assertCode(() -> service.placeItem(7L, 999L, "DISPLAY_1"), "HOME_ITEM_NOT_FOUND");
        assertCode(() -> service.storeItem(7L, 999L), "HOME_ITEM_NOT_FOUND");
        verify(topicRepository, never()).findById(any());
        verify(topicRepository, never()).saveAndFlush(any());
    }

    @Test
    void storageClearsOnlyPlacementAndIsIdempotent() {
        stubOwnedTopic();
        topic.setHomeSlotId("DISPLAY_1");
        service.storeItem(7L, 21L);
        service.storeItem(7L, 21L);
        assertThat(topic.getHomeSlotId()).isNull();
        assertThat(topic.getLearnedAt()).isEqualTo(learnedAt);
        verify(topicRepository).saveAndFlush(topic);
        verify(topicRepository, never()).delete(any());
    }

    private void stubHome() {
        when(npcRepository.findByUserId(7L)).thenReturn(Optional.of(npc));
    }

    private void stubOwnedTopic() {
        when(npcRepository.findByUserIdForUpdate(7L)).thenReturn(Optional.of(npc));
        when(topicRepository.findByIdAndNpcId(21L, 11L)).thenReturn(Optional.of(topic));
    }

    private TopicCategory category() {
        return TopicCategory.builder().id(1L).code("READING").name("読書")
            .displayName("思い出の本").itemImagePath("/images/reading.png").build();
    }

    private LevelCurve curve(int level, int exp) {
        return LevelCurve.builder().level(level).requiredExp(exp).build();
    }

    private void assertCode(Runnable action, String code) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(HomeException.class,
            exception -> assertThat(exception.getCode()).isEqualTo(code));
    }
}
