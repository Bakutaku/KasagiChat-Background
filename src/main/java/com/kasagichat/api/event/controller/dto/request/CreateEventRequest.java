package com.kasagichat.api.event.controller.dto.request;

import java.time.Instant;

import com.kasagichat.api.event.model.enums.VenueTemplate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * イベント作成のリクエスト。
 *
 * <p>開始日時と終了日時の前後関係はフィールド単体では検証できないため、Serviceで確認する。</p>
 *
 * @param title イベントのタイトル
 * @param description イベントの説明
 * @param startsAt 開催の開始日時
 * @param endsAt 開催の終了日時
 * @param venueTemplate 会場のテンプレート
 */
public record CreateEventRequest(
    @NotBlank @Size(max = 100) String title,
    @Size(max = 2000) String description,
    @NotNull Instant startsAt,
    @NotNull Instant endsAt,
    @NotNull VenueTemplate venueTemplate
) {
}
