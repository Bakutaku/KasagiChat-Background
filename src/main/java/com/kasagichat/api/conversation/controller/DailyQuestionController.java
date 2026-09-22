package com.kasagichat.api.conversation.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kasagichat.api.conversation.controller.dto.response.DailyQuestionResponse;
import com.kasagichat.api.conversation.service.ConversationService;
import com.kasagichat.api.security.principal.LoginUserPrincipal;

import lombok.RequiredArgsConstructor;

/**
 * 今日のひとことの質問を返す。
 */
@RestController
@RequestMapping("/api/daily-question")
@RequiredArgsConstructor
public class DailyQuestionController {

    private final ConversationService conversationService;

    /**
     * 未消化の質問を1件返す。まだ用意できていない場合は204を返す。
     */
    @GetMapping
    public ResponseEntity<DailyQuestionResponse> get(
        @AuthenticationPrincipal LoginUserPrincipal principal
    ) {
        return conversationService.findDailyQuestion(principal.userId())
            .map(question -> ResponseEntity.ok(new DailyQuestionResponse(question)))
            .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
