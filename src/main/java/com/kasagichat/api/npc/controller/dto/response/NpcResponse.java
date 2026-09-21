package com.kasagichat.api.npc.controller.dto.response;

import java.time.Instant;

import com.kasagichat.api.npc.model.Npc;

/**
 * 本人のNPCの基本状態。
 *
 * @param name NPCの名前
 * @param presetId 見た目のプリセットID
 * @param level 現在のレベル
 * @param exp 累計EXP
 * @param profile LLMが誕生の振り返りで生成した人格文書
 * @param speechStyle LLMが誕生の振り返りで生成した口調の特徴
 * @param speechStyleEnabled 口調を会話へ反映するか
 * @param bornAt 誕生確定日時。誕生会話の振り返り前はnull
 */
public record NpcResponse(
    String name,
    String presetId,
    Integer level,
    Integer exp,
    String profile,
    String speechStyle,
    Boolean speechStyleEnabled,
    Instant bornAt
) {

    public static NpcResponse from(Npc npc) {
        return new NpcResponse(
            npc.getName(),
            npc.getPresetId(),
            npc.getLevel(),
            npc.getExp(),
            npc.getProfile(),
            npc.getSpeechStyle(),
            npc.getSpeechStyleEnabled(),
            npc.getBornAt()
        );
    }
}
