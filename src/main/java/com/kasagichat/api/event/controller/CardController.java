package com.kasagichat.api.event.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kasagichat.api.event.controller.dto.response.CardResponse;
import com.kasagichat.api.event.service.CardService;
import com.kasagichat.api.security.principal.LoginUserPrincipal;

import lombok.RequiredArgsConstructor;

/**
 * 出会いカードの参照と開封。既存のAPI共通のROLE_USER・セッション認証・CSRF保護を使用する。
 *
 * <p>会場から見る一覧はイベント配下、スマートフォンのカード一覧と詳細はカード配下に置くため、
 * クラス単位の共通パスは持たせない。</p>
 */
@RestController
@RequiredArgsConstructor
public class CardController {
    private final CardService cardService;

    @GetMapping("/api/events/{eventId}/cards")
    public List<CardResponse> getEventCards(
            @AuthenticationPrincipal LoginUserPrincipal principal,
            @PathVariable UUID eventId
    ) {
        return cardService.listByEvent(principal.userId(), eventId);
    }

    @GetMapping("/api/cards")
    public List<CardResponse> getCards(@AuthenticationPrincipal LoginUserPrincipal principal) {
        return cardService.list(principal.userId());
    }

    @GetMapping("/api/cards/{cardId}")
    public CardResponse getCard(
            @AuthenticationPrincipal LoginUserPrincipal principal,
            @PathVariable UUID cardId
    ) {
        return cardService.get(principal.userId(), cardId);
    }

    @PostMapping("/api/cards/{cardId}/open")
    public CardResponse openCard(
            @AuthenticationPrincipal LoginUserPrincipal principal,
            @PathVariable UUID cardId
    ) {
        return cardService.open(principal.userId(), cardId);
    }
}
