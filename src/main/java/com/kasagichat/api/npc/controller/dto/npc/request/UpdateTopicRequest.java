package com.kasagichat.api.npc.controller.dto.npc.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotNull;

/**
 * 話題の公開/非公開を切り替える入力値。
 *
 * @param isPublic イベントのマッチングとカード生成に使ってよいかどうか。JSONでは {@code public}
 */
public record UpdateTopicRequest(
    @JsonProperty("public") @NotNull Boolean isPublic
) {
}
