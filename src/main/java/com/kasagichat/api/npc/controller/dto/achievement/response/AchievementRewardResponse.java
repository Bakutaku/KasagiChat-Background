package com.kasagichat.api.npc.controller.dto.achievement.response;

import com.kasagichat.api.master.model.AchievementDef;
import com.kasagichat.api.master.model.enums.RewardType;

/**
 * 実績の報酬を返却する。
 *
 * @param type 報酬の種類
 * @param exp 加算するEXP。報酬の種類がEXPの場合のみ設定する
 * @param item 解禁するアイテム。報酬の種類がITEMの場合のみ設定する
 */
public record AchievementRewardResponse(
    RewardType type,
    Integer exp,
    RewardItemResponse item
) {

    /**
     * 実績定義から報酬のレスポンスを生成する。
     *
     * @param def 実績定義
     * @return 報酬のレスポンス
     */
    public static AchievementRewardResponse from(AchievementDef def) {
        return new AchievementRewardResponse(
            def.getRewardType(),
            def.getRewardExp(),
            def.getRewardItem() == null ? null : RewardItemResponse.from(def.getRewardItem())
        );
    }
}
