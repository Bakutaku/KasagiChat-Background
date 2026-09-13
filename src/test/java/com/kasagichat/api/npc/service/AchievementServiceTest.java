package com.kasagichat.api.npc.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kasagichat.api.master.model.AchievementDef;
import com.kasagichat.api.master.model.Item;
import com.kasagichat.api.master.model.enums.ItemType;
import com.kasagichat.api.master.model.enums.RewardType;
import com.kasagichat.api.npc.controller.dto.achievement.response.AchievementResponse;
import com.kasagichat.api.npc.controller.dto.achievement.response.ClaimAchievementResponse;
import com.kasagichat.api.npc.exception.AchievementNotFoundException;
import com.kasagichat.api.npc.model.Achievement;
import com.kasagichat.api.npc.model.Npc;
import com.kasagichat.api.npc.model.UnlockedItem;
import com.kasagichat.api.npc.repository.AchievementRepository;
import com.kasagichat.api.npc.repository.NpcRepository;
import com.kasagichat.api.npc.repository.UnlockedItemRepository;
import com.kasagichat.api.security.model.Users;
import com.kasagichat.api.security.repository.UsersRepository;

@ExtendWith(MockitoExtension.class)
class AchievementServiceTest {

    @Mock
    private AchievementRepository achievementRepository;
    @Mock
    private UnlockedItemRepository unlockedItemRepository;
    @Mock
    private NpcRepository npcRepository;
    @Mock
    private UsersRepository usersRepository;
    @Mock
    private LevelService levelService;

    private AchievementService service;

    private final Users user = Users.builder().id(1L).displayName("テストユーザー").build();

    @BeforeEach
    void setUp() {
        service = new AchievementService(
                achievementRepository, unlockedItemRepository, npcRepository, usersRepository, levelService
        );
    }

    @Test
    void getAchievementsUsesUnclaimedQuery() {
        Achievement achievement = achievement(itemDef());
        when(achievementRepository.findByUserIdAndClaimedAtIsNullOrderByAchievedAtDesc(1L))
                .thenReturn(List.of(achievement));

        List<AchievementResponse> responses = service.getAchievements(1L, false);

        assertThat(responses).singleElement().satisfies(response -> {
            assertThat(response.code()).isEqualTo("FIRST_CAFE");
            assertThat(response.reward().type()).isEqualTo(RewardType.ITEM);
            assertThat(response.reward().item().code()).isEqualTo("CASUAL_SHIRT");
        });
    }

    @Test
    void claimUnlocksItemReward() {
        Achievement achievement = achievement(itemDef());
        stubClaim(1, achievement);
        when(unlockedItemRepository.existsByUserIdAndItemId(1L, 3L)).thenReturn(false);
        when(npcRepository.findByUserId(1L)).thenReturn(Optional.of(npc(2)));

        ClaimAchievementResponse response = service.claim(1L, 9L);

        ArgumentCaptor<UnlockedItem> captor = ArgumentCaptor.forClass(UnlockedItem.class);
        verify(unlockedItemRepository).save(captor.capture());
        assertThat(captor.getValue().getItem().getCode()).isEqualTo("CASUAL_SHIRT");
        assertThat(captor.getValue().getAchievement()).isSameAs(achievement);
        assertThat(captor.getValue().getUser()).isSameAs(user);
        assertThat(response.level()).isEqualTo(2);
        assertThat(response.leveledUp()).isFalse();
    }

    @Test
    void claimSkipsAlreadyUnlockedItem() {
        stubClaim(1, achievement(itemDef()));
        when(unlockedItemRepository.existsByUserIdAndItemId(1L, 3L)).thenReturn(true);

        service.claim(1L, 9L);

        verify(unlockedItemRepository, never()).save(any());
    }

    @Test
    void claimAddsExpReward() {
        AchievementDef def = AchievementDef.builder()
                .code("CONVERSATION_10")
                .name("おしゃべり好き")
                .rewardType(RewardType.EXP)
                .rewardExp(20)
                .build();
        Npc npc = npc(2);
        stubClaim(1, achievement(def));
        when(npcRepository.findByUserIdForUpdate(1L)).thenReturn(Optional.of(npc));
        when(levelService.applyExp(npc, 20)).thenReturn(new LevelUpResult(3, true));

        ClaimAchievementResponse response = service.claim(1L, 9L);

        verify(levelService).applyExp(npc, 20);
        assertThat(response.leveledUp()).isTrue();
        verifyNoInteractions(unlockedItemRepository);
    }

    @Test
    void claimDoesNotApplyRewardTwice() {
        Achievement achievement = achievement(itemDef());
        achievement.setClaimedAt(Instant.now());
        stubClaim(0, achievement);

        ClaimAchievementResponse response = service.claim(1L, 9L);

        assertThat(response.achievement().claimedAt()).isNotNull();
        assertThat(response.leveledUp()).isFalse();
        verifyNoInteractions(unlockedItemRepository, levelService);
    }

    @Test
    void claimRejectsMissingAchievement() {
        when(usersRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(user));
        when(achievementRepository.markClaimed(eq(9L), eq(1L), any(Instant.class))).thenReturn(0);
        when(achievementRepository.findByIdAndUserId(9L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.claim(1L, 9L)).isInstanceOf(AchievementNotFoundException.class);

        verifyNoInteractions(unlockedItemRepository, levelService);
    }

    private void stubClaim(int updated, Achievement achievement) {
        when(usersRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(user));
        when(achievementRepository.markClaimed(eq(9L), eq(1L), any(Instant.class))).thenReturn(updated);
        when(achievementRepository.findByIdAndUserId(9L, 1L)).thenReturn(Optional.of(achievement));
    }

    private AchievementDef itemDef() {
        return AchievementDef.builder()
                .code("FIRST_CAFE")
                .name("はじめてのカフェ")
                .rewardType(RewardType.ITEM)
                .rewardItem(Item.builder()
                        .id(3L)
                        .code("CASUAL_SHIRT")
                        .itemType(ItemType.CLOTHES)
                        .name("カジュアルシャツ")
                        .imagePath("/images/items/casual_shirt.png")
                        .build())
                .build();
    }

    private Achievement achievement(AchievementDef def) {
        return Achievement.builder()
                .id(9L)
                .user(user)
                .achievementDef(def)
                .achievedAt(Instant.now())
                .build();
    }

    private Npc npc(int level) {
        return Npc.builder().id(5L).user(user).name("ユウ").presetId("PRESET_01").level(level).build();
    }
}
