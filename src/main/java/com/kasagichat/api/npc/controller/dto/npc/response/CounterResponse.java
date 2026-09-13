package com.kasagichat.api.npc.controller.dto.npc.response;

import com.kasagichat.api.npc.model.AchievementCounter;

/**
 * 集計カウンターの値を返却する。
 *
 * @param code カウンター種別のコード
 * @param name カウンターの名称
 * @param value カウンターの現在値
 */
public record CounterResponse(
    String code,
    String name,
    Long value
) {

    /**
     * 集計カウンターからレスポンスを生成する。
     *
     * @param counter 集計カウンター
     * @return カウンターのレスポンス
     */
    public static CounterResponse from(AchievementCounter counter) {
        return new CounterResponse(
            counter.getCounterDef().getCode(),
            counter.getCounterDef().getName(),
            counter.getValue()
        );
    }
}
