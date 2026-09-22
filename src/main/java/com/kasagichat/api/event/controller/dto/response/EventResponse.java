package com.kasagichat.api.event.controller.dto.response;

import java.time.Instant;
import java.util.UUID;

import com.kasagichat.api.event.model.enums.EventPhase;
import com.kasagichat.api.event.model.enums.VenueTemplate;

/**
 * イベントの詳細。一覧と詳細で同じ形を使う。
 *
 * @param id イベントの公開ID
 * @param title イベントのタイトル
 * @param description イベントの説明
 * @param startsAt 開催の開始日時
 * @param endsAt 開催の終了日時
 * @param venueTemplate 会場のテンプレート
 * @param phase 取得時点の開催フェーズ
 * @param creatorName 作成者の分身の名前
 * @param participantCount 参加中の人数
 * @param owner 閲覧者が作成者かどうか
 * @param joined 閲覧者が参加中かどうか
 * @param inviteCode 招待コード。作成者以外にはnull
 */
public record EventResponse(
    UUID id,
    String title,
    String description,
    Instant startsAt,
    Instant endsAt,
    VenueTemplate venueTemplate,
    EventPhase phase,
    String creatorName,
    long participantCount,
    boolean owner,
    boolean joined,
    String inviteCode
) {
}
