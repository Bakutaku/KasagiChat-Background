package com.kasagichat.api.security.handler;

import com.kasagichat.api.security.SecurityProperties;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

import com.kasagichat.api.security.SecurityConst;
import com.kasagichat.api.security.model.PendingUsers;
import com.kasagichat.api.security.model.UserAuth;
import com.kasagichat.api.security.oauth.OAuthIdentity;
import com.kasagichat.api.security.oauth.OAuthIdentityResolver;
import com.kasagichat.api.security.principal.LoginUserPrincipal;
import com.kasagichat.api.security.principal.PendingUserPrincipal;
import com.kasagichat.api.security.repository.UserAuthRepository;
import com.kasagichat.api.security.service.UserRegistrationService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * OAuth認証成功後の処理
 */
@Component
@RequiredArgsConstructor
public class OAuthLoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final SecurityProperties securityProperties;
    private final OAuthIdentityResolver identityResolver;
    private final UserAuthRepository userAuthRepository;
    private final SecurityContextRepository securityContextRepository;
    private final AuthenticationFailureHandler authenticationFailureHandler;

    private final UserRegistrationService userRegistrationService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        if(!(authentication instanceof OAuth2AuthenticationToken oauthToken)) {
            SecurityContextHolder.clearContext();
            securityContextRepository.saveContext(
                    SecurityContextHolder.createEmptyContext(),
                    request,
                    response
            );
            authenticationFailureHandler.onAuthenticationFailure(
                    request,
                    response,
                    new AuthenticationServiceException("OAuth2認証情報を取得できません")
            );
            return;
        }

        // OAuth情報
        OAuthIdentity identity = identityResolver.resolve(oauthToken);
        // 認証情報
        Authentication auth;
        // リダイレクト先
        String redirectPath;

        Optional<UserAuth> existingAuth = userAuthRepository.findByProviderAndSubject(
                identity.provider(),
                identity.subject()
        );

        if(existingAuth.isPresent()) {
            LoginUserPrincipal loginUser = new LoginUserPrincipal(existingAuth.get().getUser().getId());
            auth = authenticated(loginUser,SecurityConst.ROLE_USER);
            redirectPath = "/";
        } else {
            PendingUsers pendingUser = userRegistrationService.createPendingUser(
                identity.provider(),
                identity.subject(),
                identity.displayName(),
                identity.avatarUrl()
            );
            PendingUserPrincipal principal = new PendingUserPrincipal(pendingUser.getId());
            auth = authenticated(principal, SecurityConst.ROLE_PENDING_REGISTRATION);
            redirectPath = "/signup";
        }

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);

        getRedirectStrategy().sendRedirect(request, response, securityProperties.frontendUrl().resolve(redirectPath).toString());
    }

    private Authentication authenticated(Object principal, String authority) {
        return UsernamePasswordAuthenticationToken.authenticated(
            principal,
            null,
            List.of(new SimpleGrantedAuthority(authority))
        );
    }

}
