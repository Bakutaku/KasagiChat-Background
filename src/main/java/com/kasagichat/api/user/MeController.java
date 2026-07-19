package com.kasagichat.api.user;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class MeController {

    private final AppUserService userService;

    public MeController(AppUserService userService) {
        this.userService = userService;
    }

    public record MeResponse(Long id, String provider, String displayName, String avatarUrl) {
    }

    /** ログイン中ユーザーの情報を返す。セッション/Bearerどちらの認証でも動く。未ログインは401。 */
    @GetMapping("/api/me")
    public MeResponse me(Authentication authentication) {
        AppUser user = userService.findByAuthentication(authentication)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        return new MeResponse(user.getId(), user.getProvider(), user.getDisplayName(), user.getAvatarUrl());
    }
}
