package com.kasagichat.api.npc.service;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kasagichat.api.master.model.AchievementDef;
import com.kasagichat.api.master.model.Item;
import com.kasagichat.api.npc.controller.dto.achievement.response.AchievementResponse;
import com.kasagichat.api.npc.controller.dto.achievement.response.ClaimAchievementResponse;
import com.kasagichat.api.npc.exception.AchievementNotFoundException;
import com.kasagichat.api.npc.exception.NpcNotFoundException;
import com.kasagichat.api.npc.model.Achievement;
import com.kasagichat.api.npc.model.Npc;
import com.kasagichat.api.npc.model.UnlockedItem;
import com.kasagichat.api.npc.repository.AchievementRepository;
import com.kasagichat.api.npc.repository.NpcRepository;
import com.kasagichat.api.npc.repository.UnlockedItemRepository;
import com.kasagichat.api.security.exception.UserNotFoundException;
import com.kasagichat.api.security.repository.UsersRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 実績の一覧取得と報酬の受取を行うService。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AchievementService {

    private final AchievementRepository achievementRepository;

    private final UnlockedItemRepository unlockedItemRepository;

    private final NpcRepository npcRepository;

    private final UsersRepository usersRepository;

    private final LevelService levelService;

    /**
     * ユーザーの実績を、達成日時の新しい順に取得する。
     *
     * @param userId ユーザーの内部ID
     * @param claimed trueの場合は受取済み、falseの場合は未受取、nullの場合はすべて
     * @return 実績の一覧
     */
    @Transactional(readOnly = true)
    public List<AchievementResponse> getAchievements(Long userId, Boolean claimed) {
        List<Achievement> achievements;
        if (claimed == null) {
            achievements = achievementRepository.findByUserIdOrderByAchievedAtDesc(userId);
        } else if (claimed) {
            achievements = achievementRepository.findByUserIdAndClaimedAtIsNotNullOrderByAchievedAtDesc(userId);
        } else {
            achievements = achievementRepository.findByUserIdAndClaimedAtIsNullOrderByAchievedAtDesc(userId);
        }
        return achievements.stream().map(AchievementResponse::from).toList();
    }

    /**
     * 実績の報酬を受け取る。受取済みの場合は報酬を反映せず、現在の状態を返す。
     *
     * @param userId ユーザーの内部ID
     * @param achievementId 実績達成記録の内部ID
     * @return 受取後の実績とNPCのレベル
     * @throws AchievementNotFoundException 実績が存在しない、または他人の実績の場合
     * @throws NpcNotFoundException EXP報酬を受け取るときにNPCが未作成の場合
     */
    @Transactional
    public ClaimAchievementResponse claim(Long userId, Long achievementId) {
        // 同じユーザーの報酬受取を直列化し、アイテム解禁とEXP加算の競合を防ぐ。
        usersRepository.findByIdForUpdate(userId)
            .orElseThrow(UserNotFoundException::new);

        Instant now = Instant.now();
        // 未受取の場合だけ受取日時を設定する。同時に受け取っても報酬は1回だけ反映される。
        boolean claimedNow = achievementRepository.markClaimed(achievementId, userId, now) == 1;

        Achievement achievement = achievementRepository.findByIdAndUserId(achievementId, userId)
            .orElseThrow(AchievementNotFoundException::new);
        boolean leveledUp = claimedNow && applyReward(achievement, now);

        Integer level = npcRepository.findByUserId(userId).map(Npc::getLevel).orElse(null);
        return new ClaimAchievementResponse(AchievementResponse.from(achievement), level, leveledUp);
    }

    /**
     * 実績の報酬を反映する。
     *
     * @param achievement 報酬を受け取った実績
     * @param now 受取日時
     * @return 報酬によってレベルアップしたかどうか
     */
    private boolean applyReward(Achievement achievement, Instant now) {
        AchievementDef def = achievement.getAchievementDef();
        return switch (def.getRewardType()) {
            case ITEM -> {
                unlockItem(achievement, def.getRewardItem(), now);
                yield false;
            }
            case EXP -> addExp(achievement, def.getRewardExp());
            case NONE -> false;
        };
    }

    private void unlockItem(Achievement achievement, Item item, Instant now) {
        if (item == null) {
            log.warn("報酬アイテムが設定されていない実績定義です。code: {}", achievement.getAchievementDef().getCode());
            return;
        }
        Long userId = achievement.getUser().getId();
        if (unlockedItemRepository.existsByUserIdAndItemId(userId, item.getId())) {
            return;
        }
        unlockedItemRepository.save(UnlockedItem.builder()
            .user(achievement.getUser())
            .item(item)
            .achievement(achievement)
            .unlockedAt(now)
            .build());
    }

    private boolean addExp(Achievement achievement, Integer exp) {
        if (exp == null || exp <= 0) {
            log.warn("報酬EXPが設定されていない実績定義です。code: {}", achievement.getAchievementDef().getCode());
            return false;
        }
        Npc npc = npcRepository.findByUserIdForUpdate(achievement.getUser().getId())
            .orElseThrow(NpcNotFoundException::new);
        return levelService.applyExp(npc, exp).leveledUp();
    }
}
