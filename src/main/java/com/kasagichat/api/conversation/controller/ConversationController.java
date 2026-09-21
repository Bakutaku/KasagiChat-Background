package com.kasagichat.api.conversation.controller;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kasagichat.api.conversation.controller.dto.request.SendMessageRequest;
import com.kasagichat.api.conversation.controller.dto.request.StartConversationRequest;
import com.kasagichat.api.conversation.controller.dto.response.ConversationResponse;
import com.kasagichat.api.conversation.controller.dto.response.ReviewConversationResponse;
import com.kasagichat.api.conversation.controller.dto.response.SendMessageResponse;
import com.kasagichat.api.conversation.service.BirthConversationService;
import com.kasagichat.api.conversation.service.ConversationStartResult;
import com.kasagichat.api.security.principal.LoginUserPrincipal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * NPC誕生会話の開始・取得・発言・振り返りを扱う。
 */
@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final BirthConversationService birthConversationService;

    @PostMapping
    public ResponseEntity<ConversationResponse> start(
        @AuthenticationPrincipal LoginUserPrincipal principal,
        @Valid @RequestBody StartConversationRequest request
    ) {
        ConversationStartResult result = birthConversationService.start(principal.userId(), request);
        if (!result.created()) {
            return ResponseEntity.ok(result.conversation());
        }
        return ResponseEntity.created(URI.create("/api/conversations/" + result.conversation().id()))
            .body(result.conversation());
    }

    @GetMapping("/{id}")
    public ConversationResponse get(
        @AuthenticationPrincipal LoginUserPrincipal principal,
        @PathVariable UUID id
    ) {
        return birthConversationService.get(principal.userId(), id);
    }

    @PostMapping("/{id}/messages")
    public SendMessageResponse send(
        @AuthenticationPrincipal LoginUserPrincipal principal,
        @PathVariable UUID id,
        @Valid @RequestBody SendMessageRequest request
    ) {
        return birthConversationService.send(principal.userId(), id, request);
    }

    @PostMapping("/{id}/review")
    public ReviewConversationResponse review(
        @AuthenticationPrincipal LoginUserPrincipal principal,
        @PathVariable UUID id
    ) {
        return birthConversationService.review(principal.userId(), id);
    }
}
