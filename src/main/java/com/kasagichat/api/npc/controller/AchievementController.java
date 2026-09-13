package com.kasagichat.api.npc.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kasagichat.api.npc.controller.dto.achievement.response.AchievementResponse;
import com.kasagichat.api.npc.controller.dto.achievement.response.ClaimAchievementResponse;
import com.kasagichat.api.npc.exception.AchievementNotFoundException;
import com.kasagichat.api.npc.service.AchievementService;
import com.kasagichat.api.security.principal.LoginUserPrincipal;

import lombok.RequiredArgsConstructor;

/**
 * 実績と報酬の受取を扱うController。
 */
@RestController
@RequestMapping("/api/achievements")
@RequiredArgsConstructor
public class AchievementController {

    private final AchievementService achievementService;

    /**
     * 自分の実績を取得する。
     *
     * @param principal サーバーセッションから復元されたユーザーの認証主体
     * @param claimed trueの場合は受取済み、falseの場合は未受取、未指定の場合はすべて
     * @return 実績の一覧
     */
    @GetMapping
    public List<AchievementResponse> list(
        @AuthenticationPrincipal LoginUserPrincipal principal,
        @RequestParam(required = false) Boolean claimed
    ) {
        return achievementService.getAchievements(principal.userId(), claimed);
    }

    /**
     * 実績の報酬を受け取る。受取済みの場合は報酬を反映せず、現在の状態を返す。
     *
     * @param principal サーバーセッションから復元されたユーザーの認証主体
     * @param id 実績達成記録の内部ID
     * @return 受取後の実績とNPCのレベル
     * @throws AchievementNotFoundException 実績が存在しない、または他人の実績の場合
     */
    @PostMapping("/{id}/claim")
    public ClaimAchievementResponse claim(
        @AuthenticationPrincipal LoginUserPrincipal principal,
        @PathVariable Long id
    ) {
        return achievementService.claim(principal.userId(), id);
    }
}
