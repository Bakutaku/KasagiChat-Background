package com.kasagichat.api.npc.controller.dto.npc.response;

import java.util.List;

/**
 * プロフィール帳に表示するNPCの統計を返却する。
 *
 * @param topicCount 覚えた話題の数
 * @param counters 実績判定用の集計カウンターの一覧
 */
public record NpcStatsResponse(
    long topicCount,
    List<CounterResponse> counters
) {
}
