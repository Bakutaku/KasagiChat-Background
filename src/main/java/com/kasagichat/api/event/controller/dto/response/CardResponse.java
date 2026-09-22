package com.kasagichat.api.event.controller.dto.response;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import com.kasagichat.api.event.model.Card;

/**
 * 出会いカード。一覧・詳細・開封で同じ形を使う。
 *
 * <p>カード表面（名前・見た目・共通タグ・相性スコア）はマッチングの結果だけで組み立てる。
 * 会話報告とおすすめ話題は開封時に生成するため、未開封の間はnullになる。</p>
 *
 * @param id カードの公開ID
 * @param eventId 所属イベントの公開ID
 * @param eventTitle 所属イベントのタイトル
 * @param partnerName 相手の分身の名前
 * @param partnerPresetId 相手の分身の見た目プリセットID
 * @param score 相性スコア
 * @param commonTags 共通タグ
 * @param opened 開封済みかどうか
 * @param openedAt 開封した日時。未開封はnull
 * @param report 開封時に生成した会話報告。未開封はnull
 * @param recommendedTopics 開封時に生成したおすすめ話題。未開封はnull
 */
public record CardResponse(
    UUID id,
    UUID eventId,
    String eventTitle,
    String partnerName,
    String partnerPresetId,
    Integer score,
    List<String> commonTags,
    boolean opened,
    Instant openedAt,
    String report,
    List<String> recommendedTopics
) {

    /**
     * カードのEntityからレスポンスを組み立てる。
     *
     * <p>相手の分身は連番IDを公開しないため、呼び出し側が解決した名前とプリセットIDを受け取る。</p>
     *
     * @param card カードのEntity
     * @param partnerName 相手の分身の名前
     * @param partnerPresetId 相手の分身の見た目プリセットID
     * @return レスポンス
     */
    public static CardResponse from(Card card, String partnerName, String partnerPresetId) {
        boolean opened = card.getOpenedAt() != null;
        return new CardResponse(
            card.getPublicId(),
            card.getEvent().getPublicId(),
            card.getEvent().getTitle(),
            partnerName,
            partnerPresetId,
            card.getScore(),
            toList(card.getCommonTags()),
            opened,
            card.getOpenedAt(),
            opened ? card.getReport() : null,
            opened ? toList(card.getRecommendedTopics()) : null
        );
    }

    private static List<String> toList(String[] values) {
        return values == null ? List.of() : Arrays.stream(values).toList();
    }
}
