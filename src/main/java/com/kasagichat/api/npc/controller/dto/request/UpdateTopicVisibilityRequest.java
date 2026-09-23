package com.kasagichat.api.npc.controller.dto.request;

import jakarta.validation.constraints.NotNull;

/** 話題の公開/非公開の切り替え。省略やnullは更新として扱わない。 */
public record UpdateTopicVisibilityRequest(@NotNull Boolean publicTopic) {
}
