package com.kasagichat.api.npc.controller.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.kasagichat.api.master.model.enums.ItemType;
import com.kasagichat.api.npc.model.enums.HomeItemKind;

/** 家画面の所有者専用スナップショット。永続化Entityを直接公開しない。 */
public record HomeResponse(
    NpcResponse npc,
    List<SlotResponse> slots,
    List<ItemResponse> items,
    List<UnlockedItemResponse> unlockedItems
) {
    public record NpcResponse(
        String name, String presetId, Map<String, String> appearance,
        int level, int exp, Instant bornAt, NextLevelResponse nextLevel
    ) {}

    /** nextLevel全体がnullなら、次レベルを安全に計算できるマスタがない。 */
    public record NextLevelResponse(int level, int requiredTotalExp, int remainingExp) {}

    public record SlotResponse(String slotId, HomeItemKind acceptedKind) {}

    /** topicIdが品物の識別子。カテゴリなしの場合は本として導出する。 */
    public record ItemResponse(
        Long topicId, String topicName, HomeItemKind kind,
        CategoryResponse category, String displayName, String imagePath,
        Instant acquiredAt, UUID sourceConversationId, boolean publicTopic, String slotId
    ) {}

    public record CategoryResponse(String code, String name) {}

    /** 既存の着せ替え用解禁記録。部屋配置APIのtopicIdには使用できない。 */
    public record UnlockedItemResponse(
        Long unlockedItemId, String code, ItemType itemType,
        String name, String imagePath, Instant acquiredAt
    ) {}
}
