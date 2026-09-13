package com.kasagichat.api.npc.controller.dto.achievement.response;

import java.time.Instant;

import com.kasagichat.api.master.model.AchievementDef;
import com.kasagichat.api.npc.model.Achievement;

/**
 * ユーザーが達成した実績を返却する。
 *
 * @param id 実績達成記録の内部ID
 * @param code 実績を表すコード
 * @param name 実績の名称
 * @param description 実績の説明
 * @param achievedAt 実績を達成した日時
 * @param claimedAt 報酬を受け取った日時。未受取の場合はnull
 * @param reward 実績の報酬
 */
public record AchievementResponse(
    Long id,
    String code,
    String name,
    String description,
    Instant achievedAt,
    Instant claimedAt,
    AchievementRewardResponse reward
) {

    /**
     * 実績からレスポンスを生成する。実績定義を参照するため、トランザクション内で呼び出す。
     *
     * @param achievement 実績
     * @return 実績のレスポンス
     */
    public static AchievementResponse from(Achievement achievement) {
        AchievementDef def = achievement.getAchievementDef();
        return new AchievementResponse(
            achievement.getId(),
            def.getCode(),
            def.getName(),
            def.getDescription(),
            achievement.getAchievedAt(),
            achievement.getClaimedAt(),
            AchievementRewardResponse.from(def)
        );
    }
}
