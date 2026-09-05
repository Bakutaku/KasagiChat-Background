package com.kasagichat.api.security.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kasagichat.api.security.SecurityConst;
import com.kasagichat.api.security.controller.dto.request.UserRegistrationRequest;
import com.kasagichat.api.security.controller.dto.response.PendingUserResponse;
import com.kasagichat.api.security.controller.dto.response.UserRegistrationResponse;
import com.kasagichat.api.security.model.PendingUsers;
import com.kasagichat.api.security.model.Users;
import com.kasagichat.api.security.principal.LoginUserPrincipal;
import com.kasagichat.api.security.principal.PendingUserPrincipal;
import com.kasagichat.api.security.service.UserRegistrationService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/registrations")
@RequiredArgsConstructor
public class RegistrationController {
    
    private final UserRegistrationService userRegistrationService;
    private final SecurityContextRepository securityContextRepository;

    /**
     * 仮登録ユーザーの情報を取得する
     * @param principal
     * @return {@code PendingUserResponse}
     */
    @GetMapping("/me")
    @PreAuthorize("hasAuthority('"+SecurityConst.ROLE_PENDING_REGISTRATION+"')")
    public PendingUserResponse me(@AuthenticationPrincipal PendingUserPrincipal principal) {
        // 情報取得
        PendingUsers pendingUsers = userRegistrationService.getPendingUsers(principal.pendingUserId());

        return new PendingUserResponse(pendingUsers.getDisplayName(), pendingUsers.getAvatarUrl());
    }

    @PostMapping("/complete")
    @PreAuthorize("hasAuthority('"+SecurityConst.ROLE_PENDING_REGISTRATION+"')")
    public UserRegistrationResponse complete(
        @AuthenticationPrincipal PendingUserPrincipal principal,
        @Valid @RequestBody UserRegistrationRequest registrationRequest,
        HttpServletRequest request, HttpServletResponse response
    ) {
        // 作成
        Users user = userRegistrationService.registration(principal.pendingUserId(), registrationRequest);

        // セッションIDを更新
        request.changeSessionId();

        // 認証情報の更新
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
            new LoginUserPrincipal(user.getId()), null, List.of(new SimpleGrantedAuthority(SecurityConst.ROLE_USER)));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        // サーバーセッションに保存
        securityContextRepository.saveContext(context, request, response);

        return new UserRegistrationResponse(user.getPublicId(),user.getDisplayName(),user.getAvatarUrl());
    }

}
