package com.kasagichat.api.security.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kasagichat.api.security.controller.dto.response.UserMeResponse;
import com.kasagichat.api.security.principal.LoginUserPrincipal;
import com.kasagichat.api.security.service.OnboardingService;
import com.kasagichat.api.security.service.UserService;

import lombok.RequiredArgsConstructor;

@RestController 
@RequestMapping ("/api/user")
@RequiredArgsConstructor 
public class UserController {
    
    private final UserService userService;
    private final OnboardingService onboardingService;

    /**
     * 現在認証されているユーザーの情報を取得する。
     * @param principal サーバーセッションから復元されたユーザーの認証主体
     * @return ユーザー情報
     * @throws UserNotFoundException ユーザーが見つからない場合
     */
    @GetMapping ("/me")
    public UserMeResponse me(@AuthenticationPrincipal LoginUserPrincipal principal) {
        return UserMeResponse.from(
            userService.getCurrentUser(principal.userId()),
            onboardingService.getStatus(principal.userId())
        );
        
    }
}
