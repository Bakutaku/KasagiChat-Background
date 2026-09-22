package com.kasagichat.api.event.controller.dto.response;

import java.time.Instant;
import java.util.UUID;

import com.kasagichat.api.event.model.enums.EventPhase;
import com.kasagichat.api.event.model.enums.VenueTemplate;

/**
 * 招待コードから参照するイベントの概要。
 *
 * <p>参加前のユーザーも取得するため、招待コードと参加者の一覧は含めない。</p>
 *
 * @param eventId イベントの公開ID。参加済みの場合に会場へ直接移動するために使う
 * @param title イベントのタイトル
 * @param description イベントの説明
 * @param startsAt 開催の開始日時
 * @param endsAt 開催の終了日時
 * @param venueTemplate 会場のテンプレート
 * @param phase 取得時点の開催フェーズ
 * @param creatorName 作成者の分身の名前
 * @param participantCount 参加中の人数
 * @param joined 閲覧者が参加中かどうか
 */
public record InvitationResponse(
    UUID eventId,
    String title,
    String description,
    Instant startsAt,
    Instant endsAt,
    VenueTemplate venueTemplate,
    EventPhase phase,
    String creatorName,
    long participantCount,
    boolean joined
) {
}
