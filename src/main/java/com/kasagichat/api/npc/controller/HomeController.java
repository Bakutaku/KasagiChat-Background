package com.kasagichat.api.npc.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.kasagichat.api.npc.controller.dto.request.HomePlacementRequest;
import com.kasagichat.api.npc.controller.dto.response.HomeResponse;
import com.kasagichat.api.npc.controller.dto.response.HomeResponse.ItemResponse;
import com.kasagichat.api.npc.service.HomeService;
import com.kasagichat.api.security.principal.LoginUserPrincipal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** 既存のAPI共通のROLE_USER・セッション認証・CSRF保護を使用する。 */
@RestController
@RequestMapping("/api/home")
@RequiredArgsConstructor
public class HomeController {
    private final HomeService homeService;

    @GetMapping
    public HomeResponse getHome(@AuthenticationPrincipal LoginUserPrincipal principal) {
        return homeService.getHome(principal.userId());
    }

    @PutMapping("/items/{topicId}/placement")
    public ItemResponse placeItem(
            @AuthenticationPrincipal LoginUserPrincipal principal,
            @PathVariable Long topicId,
            @Valid @RequestBody HomePlacementRequest request
    ) {
        return homeService.placeItem(principal.userId(), topicId, request.slotId());
    }

    @DeleteMapping("/items/{topicId}/placement")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void storeItem(@AuthenticationPrincipal LoginUserPrincipal principal, @PathVariable Long topicId) {
        homeService.storeItem(principal.userId(), topicId);
    }
}
