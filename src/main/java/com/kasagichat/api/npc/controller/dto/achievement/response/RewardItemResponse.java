package com.kasagichat.api.npc.controller.dto.achievement.response;

import com.kasagichat.api.master.model.Item;
import com.kasagichat.api.master.model.enums.ItemType;

/**
 * 報酬として解禁するアイテムを返却する。
 *
 * @param code アイテムの識別コード
 * @param name アイテムの表示名
 * @param itemType アイテムの種類
 * @param imagePath アイテムの画像パス
 */
public record RewardItemResponse(
    String code,
    String name,
    ItemType itemType,
    String imagePath
) {

    /**
     * アイテムからレスポンスを生成する。
     *
     * @param item アイテム
     * @return アイテムのレスポンス
     */
    public static RewardItemResponse from(Item item) {
        return new RewardItemResponse(
            item.getCode(),
            item.getName(),
            item.getItemType(),
            item.getImagePath()
        );
    }
}
