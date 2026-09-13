package com.kasagichat.api.npc.controller.dto.npc.response;

import java.time.Instant;

import com.kasagichat.api.npc.model.Npc;

/**
 * NPCのプロフィール帳の情報を返却する。
 *
 * @param name NPCの名前
 * @param presetId 見た目のプリセットID
 * @param level 現在のレベル
 * @param exp 累計EXP
 * @param nextLevelExp 次のレベルに必要な累計EXP。最大レベルの場合はnull
 * @param expToNextLevel 次のレベルまでに必要な残りEXP。最大レベルの場合はnull
 * @param profile 人格文書
 * @param speechStyle 口調の特徴を説明する文章
 * @param speechStyleEnabled 口調を会話へ反映するかどうか
 * @param bornAt 誕生した日時。未誕生の場合はnull
 * @param stats 統計
 */
public record NpcResponse(
    String name,
    String presetId,
    Integer level,
    Integer exp,
    Integer nextLevelExp,
    Integer expToNextLevel,
    String profile,
    String speechStyle,
    Boolean speechStyleEnabled,
    Instant bornAt,
    NpcStatsResponse stats
) {

    /**
     * NPCと付随する情報からレスポンスを生成する。
     *
     * @param npc NPC
     * @param nextLevelExp 次のレベルに必要な累計EXP。最大レベルの場合はnull
     * @param stats 統計
     * @return NPCのレスポンス
     */
    public static NpcResponse from(Npc npc, Integer nextLevelExp, NpcStatsResponse stats) {
        return new NpcResponse(
            npc.getName(),
            npc.getPresetId(),
            npc.getLevel(),
            npc.getExp(),
            nextLevelExp,
            nextLevelExp == null ? null : Math.max(0, nextLevelExp - npc.getExp()),
            npc.getProfile(),
            npc.getSpeechStyle(),
            npc.getSpeechStyleEnabled(),
            npc.getBornAt(),
            stats
        );
    }
}
