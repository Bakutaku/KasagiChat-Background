package com.kasagichat.api.token;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.kasagichat.api.user.AppUser;
import com.kasagichat.api.user.AppUserService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

/**
 * ネイティブクライアント(将来のUE版など)向けトークンAPI。
 *
 * 発行(/api/token)はセッションログイン済みブラウザから行い、
 * 以後クライアントは Authorization: Bearer で /api/* にアクセスする。
 */
@RestController
@RequestMapping("/api/token")
public class TokenController {

    public record TokenResponse(String tokenType, String accessToken, long expiresIn, String refreshToken) {
    }

    public record RefreshRequest(@NotBlank String refreshToken) {
    }

    private final AppUserService userService;
    private final TokenService tokenService;
    private final RefreshTokenService refreshTokenService;
    private final JwtProperties properties;

    public TokenController(AppUserService userService, TokenService tokenService,
            RefreshTokenService refreshTokenService, JwtProperties properties) {
        this.userService = userService;
        this.tokenService = tokenService;
        this.refreshTokenService = refreshTokenService;
        this.properties = properties;
    }

    /** トークン一式の発行。セッションログイン(OAuth)からのみ許可する。 */
    @PostMapping
    public TokenResponse issue(Authentication authentication) {
        if (!(authentication instanceof OAuth2AuthenticationToken oauth)) {
            // アクセストークンで新しいトークン一式を発行できると、盗まれた
            // アクセストークン1つが永続アクセス権に化けるため禁止する
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "セッションログインからのみ発行できます");
        }
        AppUser user = userService.findByLogin(oauth)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        return respond(user);
    }

    /** リフレッシュ。古いリフレッシュトークンは失効し、新しい一式に入れ替わる(ローテーション)。 */
    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
        Long userId = refreshTokenService.consume(request.refreshToken())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "リフレッシュトークンが無効です"));
        AppUser user = userService.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        return respond(user);
    }

    /** ネイティブクライアントのログアウト。以後このリフレッシュトークンは使えない。 */
    @PostMapping("/revoke")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@Valid @RequestBody RefreshRequest request) {
        refreshTokenService.revoke(request.refreshToken());
    }

    private TokenResponse respond(AppUser user) {
        return new TokenResponse(
                "Bearer",
                tokenService.issueAccessToken(user),
                properties.accessTokenTtl().toSeconds(),
                refreshTokenService.issue(user.getId()));
    }
}
