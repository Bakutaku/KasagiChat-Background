package com.kasagichat.api.npc.controller.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import com.kasagichat.api.master.model.enums.ItemType;
import com.kasagichat.api.npc.model.Topic;
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

    /**
     * topicIdが品物の識別子。カテゴリなしの場合は本として導出する。
     *
     * <p>家の一覧とプロフィール帳の話題更新で同じ形を返すため、変換はここに集約する。</p>
     */
    public record ItemResponse(
        Long topicId, String topicName, HomeItemKind kind,
        CategoryResponse category, String displayName, String imagePath,
        Instant acquiredAt, UUID sourceConversationId, boolean publicTopic, String slotId
    ) {

        /**
         * 話題から所有者向けの品物表現を作る。
         *
         * @param topic 変換する話題
         * @param userId 閲覧している本人の内部ID
         * @return 品物表現
         */
        public static ItemResponse from(Topic topic, Long userId) {
            var category = topic.getCategory();
            var conversation = topic.getSourceConversation();
            // 過去データの不整合があっても他人の会話IDを漏らさない。
            UUID conversationId = conversation != null
                    && conversation.getUser() != null
                    && Objects.equals(conversation.getUser().getId(), userId)
                ? conversation.getPublicId() : null;
            return new ItemResponse(
                topic.getId(), topic.getName(), kindOf(topic),
                category == null ? null : new CategoryResponse(category.getCode(), category.getName()),
                category == null ? topic.getName() : category.getDisplayName(),
                category == null ? null : category.getItemImagePath(),
                topic.getLearnedAt(), conversationId, Boolean.TRUE.equals(topic.getPublicTopic()), topic.getHomeSlotId()
            );
        }

        /**
         * 話題の品物種別を導出する。カテゴリのない話題は汎用スロットの本として扱う。
         *
         * @param topic 判定する話題
         * @return 品物種別
         */
        public static HomeItemKind kindOf(Topic topic) {
            return topic.getCategory() == null ? HomeItemKind.BOOK : HomeItemKind.SOUVENIR;
        }
    }

    public record CategoryResponse(String code, String name) {}

    /** 既存の着せ替え用解禁記録。部屋配置APIのtopicIdには使用できない。 */
    public record UnlockedItemResponse(
        Long unlockedItemId, String code, ItemType itemType,
        String name, String imagePath, Instant acquiredAt
    ) {}
}
