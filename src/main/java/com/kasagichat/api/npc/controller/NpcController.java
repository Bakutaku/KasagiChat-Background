package com.kasagichat.api.npc.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.kasagichat.api.npc.controller.dto.request.CreateNpcRequest;
import com.kasagichat.api.npc.controller.dto.response.NpcResponse;
import com.kasagichat.api.npc.service.NpcService;
import com.kasagichat.api.security.principal.LoginUserPrincipal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 認証済みユーザー本人のNPCを扱う。
 */
@RestController
@RequestMapping("/api/npc")
@RequiredArgsConstructor
public class NpcController {

    private final NpcService npcService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NpcResponse create(
        @AuthenticationPrincipal LoginUserPrincipal principal,
        @Valid @RequestBody CreateNpcRequest request
    ) {
        return npcService.create(principal.userId(), request);
    }

    @GetMapping
    public NpcResponse get(@AuthenticationPrincipal LoginUserPrincipal principal) {
        return npcService.get(principal.userId());
    }
}
