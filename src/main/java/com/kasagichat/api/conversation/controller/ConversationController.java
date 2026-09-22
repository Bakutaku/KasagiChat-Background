package com.kasagichat.api.conversation.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kasagichat.api.conversation.controller.dto.request.ConversationQueryStatus;
import com.kasagichat.api.conversation.controller.dto.request.SendMessageRequest;
import com.kasagichat.api.conversation.controller.dto.request.StartConversationRequest;
import com.kasagichat.api.conversation.controller.dto.response.ConversationResponse;
import com.kasagichat.api.conversation.controller.dto.response.ConversationSummaryResponse;
import com.kasagichat.api.conversation.controller.dto.response.ReviewConversationResponse;
import com.kasagichat.api.conversation.controller.dto.response.SendMessageResponse;
import com.kasagichat.api.conversation.service.ConversationService;
import com.kasagichat.api.conversation.service.ConversationStartResult;
import com.kasagichat.api.security.principal.LoginUserPrincipal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 会話の開始・取得・発言・振り返りを扱う。
 */
@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;

    @PostMapping
    public ResponseEntity<ConversationResponse> start(
        @AuthenticationPrincipal LoginUserPrincipal principal,
        @Valid @RequestBody StartConversationRequest request
    ) {
        ConversationStartResult result = conversationService.start(principal.userId(), request);
        if (!result.created()) {
            return ResponseEntity.ok(result.conversation());
        }
        return ResponseEntity.created(URI.create("/api/conversations/" + result.conversation().id()))
            .body(result.conversation());
    }

    /**
     * 未振り返りの会話を新しい順に返す。家の一覧から会話を再開するために使う。
     */
    @GetMapping
    public List<ConversationSummaryResponse> list(
        @AuthenticationPrincipal LoginUserPrincipal principal,
        @RequestParam ConversationQueryStatus status
    ) {
        return conversationService.findUnreviewed(principal.userId());
    }

    @GetMapping("/{id}")
    public ConversationResponse get(
        @AuthenticationPrincipal LoginUserPrincipal principal,
        @PathVariable UUID id
    ) {
        return conversationService.get(principal.userId(), id);
    }

    @PostMapping("/{id}/messages")
    public SendMessageResponse send(
        @AuthenticationPrincipal LoginUserPrincipal principal,
        @PathVariable UUID id,
        @Valid @RequestBody SendMessageRequest request
    ) {
        return conversationService.send(principal.userId(), id, request);
    }

    @PostMapping("/{id}/review")
    public ReviewConversationResponse review(
        @AuthenticationPrincipal LoginUserPrincipal principal,
        @PathVariable UUID id
    ) {
        return conversationService.review(principal.userId(), id);
    }
}
