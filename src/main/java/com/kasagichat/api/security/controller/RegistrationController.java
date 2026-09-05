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
import com.kasagichat.api.security.exception.PendingRegistrationExpiredException;
import com.kasagichat.api.security.exception.PendingRegistrationNotFoundException;
import com.kasagichat.api.security.exception.TermsAgreementRequiredException;
import com.kasagichat.api.security.exception.UserAlreadyRegisteredException;
import com.kasagichat.api.security.model.PendingUsers;
import com.kasagichat.api.security.model.Users;
import com.kasagichat.api.security.principal.LoginUserPrincipal;
import com.kasagichat.api.security.principal.PendingUserPrincipal;
import com.kasagichat.api.security.service.UserRegistrationService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 仮登録ユーザーの情報取得と本登録を受け付けるController。
 */
@RestController
@RequestMapping("/api/registrations")
@RequiredArgsConstructor
public class RegistrationController {
    
    private final UserRegistrationService userRegistrationService;

    private final SecurityContextRepository securityContextRepository;

    /**
     * 現在認証されている仮登録ユーザーの情報を取得する。
     *
     * @param principal サーバーセッションから復元された仮登録ユーザーの認証主体
     * @return アカウント作成画面に表示する仮登録ユーザー情報
     * @throws PendingRegistrationNotFoundException 仮登録情報が存在しない場合
     * @throws PendingRegistrationExpiredException 仮登録情報が期限切れの場合
     */
    @GetMapping("/me")
    @PreAuthorize("hasAuthority('"+SecurityConst.ROLE_PENDING_REGISTRATION+"')")
    public PendingUserResponse me(@AuthenticationPrincipal PendingUserPrincipal principal) {
        PendingUsers pendingUsers = userRegistrationService.getPendingUsers(principal.pendingUserId());

        return new PendingUserResponse(pendingUsers.getDisplayName(), pendingUsers.getAvatarUrl());
    }

    /**
     * 仮登録ユーザーを本登録し、登録済みユーザーとして認証情報を更新する。
     *
     * @param principal サーバーセッションから復元された仮登録ユーザーの認証主体
     * @param registrationRequest 表示名と同意した規約IDを含む登録リクエスト
     * @param request セッションID更新に使用するHTTPリクエスト
     * @param response 認証情報保存に使用するHTTPレスポンス
     * @return 登録されたユーザー情報
     * @throws PendingRegistrationNotFoundException 仮登録情報が存在しない場合
     * @throws PendingRegistrationExpiredException 仮登録情報が期限切れの場合
     * @throws UserAlreadyRegisteredException OAuthアカウントが登録済みの場合
     * @throws TermsAgreementRequiredException 最新規約への同意が不足している場合
     */
    @PostMapping("/complete")
    @PreAuthorize("hasAuthority('"+SecurityConst.ROLE_PENDING_REGISTRATION+"')")
    public UserRegistrationResponse complete(
        @AuthenticationPrincipal PendingUserPrincipal principal,
        @Valid @RequestBody UserRegistrationRequest registrationRequest,
        HttpServletRequest request, HttpServletResponse response
    ) {
        Users user = userRegistrationService.registration(principal.pendingUserId(), registrationRequest);

        // セッション固定攻撃を防ぐため、登録完了時にセッションIDを更新する。
        request.changeSessionId();

        // 仮登録用の認証情報を登録済みユーザーの認証情報へ置き換える。
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
            new LoginUserPrincipal(user.getId()), null, List.of(new SimpleGrantedAuthority(SecurityConst.ROLE_USER)));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        // 更新した認証情報をサーバーセッションへ保存する。
        securityContextRepository.saveContext(context, request, response);

        return new UserRegistrationResponse(user.getPublicId(),user.getDisplayName(),user.getAvatarUrl());
    }

}
