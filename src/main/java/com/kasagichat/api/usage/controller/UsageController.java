package com.kasagichat.api.usage.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kasagichat.api.security.principal.LoginUserPrincipal;
import com.kasagichat.api.usage.controller.dto.usage.response.UsageSummaryResponse;
import com.kasagichat.api.usage.service.UsageService;

import lombok.RequiredArgsConstructor;

/**
 * LLMの利用状況を提供するController。
 */
@RestController
@RequestMapping("/api/usage")
@RequiredArgsConstructor
public class UsageController {

    private final UsageService usageService;

    /**
     * 自分のLLM呼び出し回数とコストの概算を取得する。
     *
     * @param principal サーバーセッションから復元されたユーザーの認証主体
     * @return LLMの利用状況
     */
    @GetMapping("/summary")
    public UsageSummaryResponse summary(@AuthenticationPrincipal LoginUserPrincipal principal) {
        return usageService.getSummary(principal.userId());
    }
}
