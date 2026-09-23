package com.kasagichat.api.npc.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.kasagichat.api.npc.controller.dto.request.UpdateTopicVisibilityRequest;
import com.kasagichat.api.npc.controller.dto.response.HomeResponse.ItemResponse;
import com.kasagichat.api.npc.service.TopicService;
import com.kasagichat.api.security.principal.LoginUserPrincipal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * プロフィール帳から本人の話題を管理する。
 *
 * <p>一覧は{@code GET /api/home}の{@code items}を流用するため、ここでは提供しない。
 * 既存のAPI共通のROLE_USER・セッション認証・CSRF保護を使用する。</p>
 */
@RestController
@RequestMapping("/api/topics")
@RequiredArgsConstructor
public class TopicController {

    private final TopicService topicService;

    @PatchMapping("/{topicId}")
    public ItemResponse updateVisibility(
        @AuthenticationPrincipal LoginUserPrincipal principal,
        @PathVariable Long topicId,
        @Valid @RequestBody UpdateTopicVisibilityRequest request
    ) {
        return topicService.updateVisibility(principal.userId(), topicId, request.publicTopic());
    }

    @DeleteMapping("/{topicId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal LoginUserPrincipal principal, @PathVariable Long topicId) {
        topicService.delete(principal.userId(), topicId);
    }
}
