package com.kasagichat.api.npc.controller.dto.growthevent.response;

import java.time.Instant;

import com.kasagichat.api.npc.model.GrowthEvent;
import com.kasagichat.api.npc.model.enums.GrowthEventType;

/**
 * 成長演出・実績達成の通知を返却する。
 *
 * @param id 通知の内部ID
 * @param type 通知の種類
 * @param message 画面に表示する演出テキスト
 * @param createdAt 通知が作成された日時
 * @param readAt 既読にした日時。未読の場合はnull
 */
public record GrowthEventResponse(
    Long id,
    GrowthEventType type,
    String message,
    Instant createdAt,
    Instant readAt
) {

    /**
     * 通知からレスポンスを生成する。
     *
     * @param growthEvent 通知
     * @return 通知のレスポンス
     */
    public static GrowthEventResponse from(GrowthEvent growthEvent) {
        return new GrowthEventResponse(
            growthEvent.getId(),
            growthEvent.getType(),
            growthEvent.getMessage(),
            growthEvent.getCreatedAt(),
            growthEvent.getReadAt()
        );
    }
}
