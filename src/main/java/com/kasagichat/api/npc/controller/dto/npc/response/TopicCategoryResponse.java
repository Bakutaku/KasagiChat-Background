package com.kasagichat.api.npc.controller.dto.npc.response;

import com.kasagichat.api.master.model.TopicCategory;

/**
 * 話題のカテゴリと、家に置く思い出の品を返却する。
 *
 * @param code カテゴリの識別コード
 * @param name カテゴリ名
 * @param displayName 思い出の品の表示名
 * @param itemImagePath 思い出の品の画像パス
 */
public record TopicCategoryResponse(
    String code,
    String name,
    String displayName,
    String itemImagePath
) {

    /**
     * 話題カテゴリからレスポンスを生成する。
     *
     * @param category 話題カテゴリ
     * @return カテゴリのレスポンス
     */
    public static TopicCategoryResponse from(TopicCategory category) {
        return new TopicCategoryResponse(
            category.getCode(),
            category.getName(),
            category.getDisplayName(),
            category.getItemImagePath()
        );
    }
}
