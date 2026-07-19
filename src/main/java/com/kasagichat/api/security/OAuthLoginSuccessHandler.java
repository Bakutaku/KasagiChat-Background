package com.kasagichat.api.security;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.kasagichat.api.user.AppUserService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * OAuthログイン成功時: usersテーブルへ登録/更新してからトップへリダイレクト。
 */
@Component
public class OAuthLoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final AppUserService userService;

    public OAuthLoginSuccessHandler(AppUserService userService,
            @Value("${app.public-url}") String publicUrl) {
        this.userService = userService;
        // 相対URL("/")はTomcatがプロキシ側のHost(backend:8080)で絶対URL化してしまうため、
        // ブラウザから見える公開URLで明示的に指定する
        setDefaultTargetUrl(publicUrl + "/");
        setAlwaysUseDefaultTargetUrl(true);
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        if (authentication instanceof OAuth2AuthenticationToken token) {
            userService.upsertFromLogin(token);
        }
        super.onAuthenticationSuccess(request, response, authentication);
    }
}
