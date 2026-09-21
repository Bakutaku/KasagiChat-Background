package com.kasagichat.api.npc.service;

import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kasagichat.api.master.model.LevelCurve;
import com.kasagichat.api.master.repository.LevelCurveRepository;
import com.kasagichat.api.npc.controller.dto.response.HomeResponse;
import com.kasagichat.api.npc.controller.dto.response.HomeResponse.CategoryResponse;
import com.kasagichat.api.npc.controller.dto.response.HomeResponse.ItemResponse;
import com.kasagichat.api.npc.controller.dto.response.HomeResponse.NextLevelResponse;
import com.kasagichat.api.npc.controller.dto.response.HomeResponse.NpcResponse;
import com.kasagichat.api.npc.controller.dto.response.HomeResponse.SlotResponse;
import com.kasagichat.api.npc.controller.dto.response.HomeResponse.UnlockedItemResponse;
import com.kasagichat.api.npc.exception.HomeException;
import com.kasagichat.api.npc.model.Npc;
import com.kasagichat.api.npc.model.Topic;
import com.kasagichat.api.npc.model.enums.HomeItemKind;
import com.kasagichat.api.npc.model.enums.HomeSlot;
import com.kasagichat.api.npc.repository.NpcRepository;
import com.kasagichat.api.npc.repository.TopicRepository;
import com.kasagichat.api.npc.repository.UnlockedItemRepository;
import com.kasagichat.api.security.service.UserService;

import lombok.RequiredArgsConstructor;

/** 話題由来の品物を表示・配置する。取得時の品物作成や自動配置は行わない。 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeService {
    private final UserService userService;
    private final NpcRepository npcRepository;
    private final TopicRepository topicRepository;
    private final UnlockedItemRepository unlockedItemRepository;
    private final LevelCurveRepository levelCurveRepository;

    public HomeResponse getHome(Long userId) {
        userService.getCurrentUser(userId);
        Npc npc = npcRepository.findByUserId(userId).orElseThrow(HomeException::npcNotFound);
        var items = topicRepository.findByNpcIdOrderByLearnedAtDescIdDesc(npc.getId()).stream()
            .map(topic -> toItem(topic, userId)).toList();
        var unlockedItems = unlockedItemRepository.findByUserIdOrderByUnlockedAtDescIdDesc(userId).stream()
            .map(unlocked -> new UnlockedItemResponse(
                unlocked.getId(), unlocked.getItem().getCode(), unlocked.getItem().getItemType(),
                unlocked.getItem().getName(), unlocked.getItem().getImagePath(), unlocked.getUnlockedAt()
            )).toList();
        return new HomeResponse(
            new NpcResponse(npc.getName(), npc.getPresetId(), npc.getAppearance(),
                npc.getLevel(), npc.getExp(), npc.getBornAt(), nextLevel(npc)),
            Arrays.stream(HomeSlot.values())
                .map(slot -> new SlotResponse(slot.name(), slot.acceptedKind())).toList(),
            items, unlockedItems
        );
    }

    /** 配置と移動は同じ操作。同じ配置先への再送は成功させ、占有済みなら元の配置を保つ。 */
    @Transactional
    public ItemResponse placeItem(Long userId, Long topicId, String slotId) {
        Npc npc = lockNpc(userId);
        Topic topic = ownedTopic(npc, topicId);
        HomeSlot slot = HomeSlot.fromId(slotId).orElseThrow(HomeException::invalidSlot);
        if (slot.acceptedKind() != kind(topic)) {
            throw HomeException.incompatibleSlot();
        }
        if (!Objects.equals(topic.getHomeSlotId(), slotId)) {
            if (topicRepository.existsByNpcIdAndHomeSlotIdAndIdNot(npc.getId(), slotId, topicId)) {
                throw HomeException.occupiedSlot();
            }
            topic.setHomeSlotId(slotId);
            topicRepository.saveAndFlush(topic);
        }
        return toItem(topic, userId);
    }

    /** 収納は配置先を消すだけで、話題・思い出・獲得日時を変更しない。 */
    @Transactional
    public void storeItem(Long userId, Long topicId) {
        Npc npc = lockNpc(userId);
        Topic topic = ownedTopic(npc, topicId);
        if (topic.getHomeSlotId() != null) {
            topic.setHomeSlotId(null);
            topicRepository.saveAndFlush(topic);
        }
    }

    private Npc lockNpc(Long userId) {
        userService.getCurrentUser(userId);
        return npcRepository.findByUserIdForUpdate(userId).orElseThrow(HomeException::npcNotFound);
    }

    private Topic ownedTopic(Npc npc, Long topicId) {
        return topicRepository.findByIdAndNpcId(topicId, npc.getId())
            .orElseThrow(HomeException::itemNotFound);
    }

    private HomeItemKind kind(Topic topic) {
        return topic.getCategory() == null ? HomeItemKind.BOOK : HomeItemKind.SOUVENIR;
    }

    private ItemResponse toItem(Topic topic, Long userId) {
        var category = topic.getCategory();
        var conversation = topic.getSourceConversation();
        // 過去データの不整合があっても他人の会話IDを漏らさない。
        UUID conversationId = conversation != null
                && conversation.getUser() != null
                && Objects.equals(conversation.getUser().getId(), userId)
            ? conversation.getPublicId() : null;
        return new ItemResponse(
            topic.getId(), topic.getName(), kind(topic),
            category == null ? null : new CategoryResponse(category.getCode(), category.getName()),
            category == null ? topic.getName() : category.getDisplayName(),
            category == null ? null : category.getItemImagePath(),
            topic.getLearnedAt(), conversationId, Boolean.TRUE.equals(topic.getPublicTopic()), topic.getHomeSlotId()
        );
    }

    private NextLevelResponse nextLevel(Npc npc) {
        if (npc.getLevel() < 1 || npc.getLevel() == Integer.MAX_VALUE || npc.getExp() < 0) {
            return null;
        }
        LevelCurve current = levelCurveRepository.findById(npc.getLevel()).orElse(null);
        LevelCurve next = levelCurveRepository.findById(npc.getLevel() + 1).orElse(null);
        // 欠落・逆転・現在状態と不整合なマスタから、推測で必要EXPを作らない。
        if (current == null || next == null || current.getRequiredExp() == null || next.getRequiredExp() == null
                || current.getRequiredExp() < 0 || current.getRequiredExp() > npc.getExp()
                || next.getRequiredExp() <= npc.getExp()) {
            return null;
        }
        return new NextLevelResponse(next.getLevel(), next.getRequiredExp(), next.getRequiredExp() - npc.getExp());
    }
}
