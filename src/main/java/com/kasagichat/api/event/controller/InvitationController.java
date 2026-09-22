package com.kasagichat.api.event.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kasagichat.api.event.controller.dto.response.EventResponse;
import com.kasagichat.api.event.controller.dto.response.InvitationResponse;
import com.kasagichat.api.event.service.EventService;
import com.kasagichat.api.security.principal.LoginUserPrincipal;

import lombok.RequiredArgsConstructor;

/** 招待コードからの参照と参加。捨てアカウントを避けるため、ログイン後のみ利用できる。 */
@RestController
@RequestMapping("/api/invitations")
@RequiredArgsConstructor
public class InvitationController {
    private final EventService eventService;

    @GetMapping("/{inviteCode}")
    public InvitationResponse getInvitation(
            @AuthenticationPrincipal LoginUserPrincipal principal,
            @PathVariable String inviteCode
    ) {
        return eventService.invitation(principal.userId(), inviteCode);
    }

    @PostMapping("/{inviteCode}/join")
    public EventResponse join(
            @AuthenticationPrincipal LoginUserPrincipal principal,
            @PathVariable String inviteCode
    ) {
        return eventService.join(principal.userId(), inviteCode);
    }
}
