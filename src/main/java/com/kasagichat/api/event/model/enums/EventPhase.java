package com.kasagichat.api.event.model.enums;

import java.time.Instant;

/**
 * イベントの開催フェーズ。
 *
 * <p>列として保持せず、開始・終了日時と早期終了日時から算出する。</p>
 */
public enum EventPhase {
    /**
     * 開催前。
     */
    UPCOMING,

    /**
     * 開催中。
     */
    ONGOING,

    /**
     * 終了済み。会期終了と作成者による早期終了の両方を含む。
     */
    ENDED;

    /**
     * 指定時点でのフェーズを算出する。
     *
     * @param startsAt 開催の開始日時
     * @param endsAt 開催の終了日時
     * @param archivedAt 作成者が早期終了した日時。終了していない場合はnull
     * @param now 判定の基準時刻
     * @return 基準時刻でのフェーズ
     */
    public static EventPhase of(Instant startsAt, Instant endsAt, Instant archivedAt, Instant now) {
        if (archivedAt != null || now.isAfter(endsAt)) {
            return ENDED;
        }
        // 開始日時ちょうどは開催中として扱う。
        return now.isBefore(startsAt) ? UPCOMING : ONGOING;
    }
}
