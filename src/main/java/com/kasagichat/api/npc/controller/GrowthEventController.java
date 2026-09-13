package com.kasagichat.api.npc.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.kasagichat.api.npc.controller.dto.growthevent.response.GrowthEventResponse;
import com.kasagichat.api.npc.service.GrowthEventService;
import com.kasagichat.api.security.principal.LoginUserPrincipal;

import lombok.RequiredArgsConstructor;

/**
 * 成長演出・実績達成の通知を扱うController。
 */
@RestController
@RequestMapping("/api/growth-events")
@RequiredArgsConstructor
public class GrowthEventController {

    private final GrowthEventService growthEventService;

    /**
     * 自分宛ての通知を取得する。
     *
     * @param principal サーバーセッションから復元されたユーザーの認証主体
     * @param unread trueの場合は未読の通知を古い順に、falseの場合は直近50件を新しい順に取得する
     * @return 通知の一覧
     */
    @GetMapping
    public List<GrowthEventResponse> list(
        @AuthenticationPrincipal LoginUserPrincipal principal,
        @RequestParam(defaultValue = "false") boolean unread
    ) {
        return growthEventService.getGrowthEvents(principal.userId(), unread);
    }

    /**
     * 自分宛ての未読の通知をすべて既読にする。
     *
     * @param principal サーバーセッションから復元されたユーザーの認証主体
     */
    @PostMapping("/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAllAsRead(@AuthenticationPrincipal LoginUserPrincipal principal) {
        growthEventService.markAllAsRead(principal.userId());
    }
}
