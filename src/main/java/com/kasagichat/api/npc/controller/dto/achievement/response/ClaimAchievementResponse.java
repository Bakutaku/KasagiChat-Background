package com.kasagichat.api.npc.controller.dto.achievement.response;

/**
 * 実績の報酬を受け取った結果を返却する。
 *
 * @param achievement 受取後の実績
 * @param level 受取後のNPCのレベル。NPCが未作成の場合はnull
 * @param leveledUp 今回の受取でレベルアップしたかどうか
 */
public record ClaimAchievementResponse(
    AchievementResponse achievement,
    Integer level,
    boolean leveledUp
) {
}
