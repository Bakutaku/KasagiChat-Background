package com.kasagichat.api.npc.controller.dto.npc.response;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kasagichat.api.npc.model.Topic;

/**
 * NPCが覚えた話題を返却する。
 *
 * @param id 話題の内部ID
 * @param name 話題名
 * @param category 話題のカテゴリ。どのカテゴリにも入らない場合はnull
 * @param interest 話題への興味度
 * @param isPublic イベントのマッチングとカード生成に使ってよいかどうか。JSONでは {@code public}
 * @param visibilityDecidedAt ユーザーが公開/非公開を決めた日時。未確認の場合はnull
 * @param learnedAt 話題を覚えた日時
 */
public record TopicResponse(
    Long id,
    String name,
    TopicCategoryResponse category,
    Short interest,
    @JsonProperty("public") boolean isPublic,
    Instant visibilityDecidedAt,
    Instant learnedAt
) {

    /**
     * 話題からレスポンスを生成する。カテゴリを参照するため、トランザクション内で呼び出す。
     *
     * @param topic 話題
     * @return 話題のレスポンス
     */
    public static TopicResponse from(Topic topic) {
        return new TopicResponse(
            topic.getId(),
            topic.getName(),
            topic.getCategory() == null ? null : TopicCategoryResponse.from(topic.getCategory()),
            topic.getInterest(),
            Boolean.TRUE.equals(topic.getPublicTopic()),
            topic.getVisibilityDecidedAt(),
            topic.getLearnedAt()
        );
    }
}
