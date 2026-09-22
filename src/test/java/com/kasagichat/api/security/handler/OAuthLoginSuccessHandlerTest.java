package com.kasagichat.api.security.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.Duration;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.context.SecurityContextRepository;

import com.kasagichat.api.security.SecurityConst;
import com.kasagichat.api.security.SecurityProperties;
import com.kasagichat.api.security.model.PendingUsers;
import com.kasagichat.api.security.model.UserAuth;
import com.kasagichat.api.security.model.Users;
import com.kasagichat.api.security.model.enums.AuthProvider;
import com.kasagichat.api.security.oauth.OAuthIdentity;
import com.kasagichat.api.security.oauth.OAuthIdentityResolver;
import com.kasagichat.api.security.principal.LoginUserPrincipal;
import com.kasagichat.api.security.principal.PendingUserPrincipal;
import com.kasagichat.api.security.repository.UserAuthRepository;
import com.kasagichat.api.security.service.UserRegistrationService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

class OAuthLoginSuccessHandlerTest {

    private OAuthIdentityResolver identityResolver;
    private UserAuthRepository userAuthRepository;
    private SecurityContextRepository securityContextRepository;
    private AuthenticationFailureHandler authenticationFailureHandler;
    private UserRegistrationService userRegistrationService;
    private RedirectStrategy redirectStrategy;
    private OAuthLoginSuccessHandler handler;
    private HttpServletRequest request;
    private HttpServletResponse response;

    @BeforeEach
    void setUp() {
    identityResolver = mock(OAuthIdentityResolver.class);
    userAuthRepository = mock(UserAuthRepository.class);
    securityContextRepository = mock(SecurityContextRepository.class);
    authenticationFailureHandler = mock(AuthenticationFailureHandler.class);
    userRegistrationService = mock(UserRegistrationService.class);
    redirectStrategy = mock(RedirectStrategy.class);
    request = mock(HttpServletRequest.class);
    response = mock(HttpServletResponse.class);

    handler = new OAuthLoginSuccessHandler(
        new SecurityProperties(URI.create("http://localhost:3000"), Duration.ofMinutes(30)),
        identityResolver,
        userAuthRepository,
        securityContextRepository,
        authenticationFailureHandler,
        userRegistrationService
    );
    handler.setRedirectStrategy(redirectStrategy);
    }

    @AfterEach
    void tearDown() {
    SecurityContextHolder.clearContext();
    }

    @Test
    void delegatesUnexpectedAuthenticationToFailureHandlerAndClearsContext() throws Exception {
    Authentication unexpectedAuthentication =
        UsernamePasswordAuthenticationToken.authenticated("user", null, java.util.List.of());
    SecurityContext initialContext = SecurityContextHolder.createEmptyContext();
    initialContext.setAuthentication(unexpectedAuthentication);
    SecurityContextHolder.setContext(initialContext);

    handler.onAuthenticationSuccess(request, response, unexpectedAuthentication);

    ArgumentCaptor<SecurityContext> contextCaptor = ArgumentCaptor.forClass(SecurityContext.class);
    verify(securityContextRepository).saveContext(contextCaptor.capture(), any(), any());
    assertThat(contextCaptor.getValue().getAuthentication()).isNull();
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

    ArgumentCaptor<AuthenticationServiceException> exceptionCaptor =
        ArgumentCaptor.forClass(AuthenticationServiceException.class);
    verify(authenticationFailureHandler)
        .onAuthenticationFailure(any(), any(), exceptionCaptor.capture());
    assertThat(exceptionCaptor.getValue().getMessage())
        .isEqualTo("OAuth2認証情報を取得できません");
    verifyNoInteractions(identityResolver, userAuthRepository, userRegistrationService, redirectStrategy);
    }

    @Test
    void redirectsAnExistingUserAndStoresApplicationAuthentication() throws Exception {
    OAuth2AuthenticationToken oauthToken = mock(OAuth2AuthenticationToken.class);
    OAuthIdentity identity = identity();
    Users user = Users.builder().id(25L).displayName("Existing").build();
    UserAuth userAuth = UserAuth.builder()
        .user(user)
        .provider(AuthProvider.GOOGLE)
        .subject("subject")
        .build();
    when(identityResolver.resolve(oauthToken)).thenReturn(identity);
    when(userAuthRepository.findByProviderAndSubject(AuthProvider.GOOGLE, "subject"))
        .thenReturn(Optional.of(userAuth));

    handler.onAuthenticationSuccess(request, response, oauthToken);

    SecurityContext context = savedContext();
    assertThat(context.getAuthentication().getPrincipal())
        .isEqualTo(new LoginUserPrincipal(25L));
    assertThat(context.getAuthentication().getAuthorities())
        .extracting("authority")
        .containsExactly(SecurityConst.ROLE_USER);
    verify(redirectStrategy)
        .sendRedirect(request, response, "http://localhost:3000/home");
    verifyNoInteractions(authenticationFailureHandler, userRegistrationService);
    }

    @Test
    void redirectsANewUserToSignupAndStoresPendingAuthentication() throws Exception {
    OAuth2AuthenticationToken oauthToken = mock(OAuth2AuthenticationToken.class);
    OAuthIdentity identity = identity();
    PendingUsers pendingUser = PendingUsers.builder().id(30L).build();
    when(identityResolver.resolve(oauthToken)).thenReturn(identity);
    when(userAuthRepository.findByProviderAndSubject(AuthProvider.GOOGLE, "subject"))
        .thenReturn(Optional.empty());
    when(userRegistrationService.createPendingUser(
        AuthProvider.GOOGLE,
        "subject",
        "Display Name",
        "https://example.com/avatar.png"
    )).thenReturn(pendingUser);

    handler.onAuthenticationSuccess(request, response, oauthToken);

    SecurityContext context = savedContext();
    assertThat(context.getAuthentication().getPrincipal())
        .isEqualTo(new PendingUserPrincipal(30L));
    assertThat(context.getAuthentication().getAuthorities())
        .extracting("authority")
        .containsExactly(SecurityConst.ROLE_PENDING_REGISTRATION);
    verify(redirectStrategy)
        .sendRedirect(request, response, "http://localhost:3000/onboarding");
    verifyNoInteractions(authenticationFailureHandler);
    }

    private OAuthIdentity identity() {
    return new OAuthIdentity(
        AuthProvider.GOOGLE,
        "subject",
        "Display Name",
        "https://example.com/avatar.png"
    );
    }

    private SecurityContext savedContext() {
    ArgumentCaptor<SecurityContext> contextCaptor = ArgumentCaptor.forClass(SecurityContext.class);
    verify(securityContextRepository).saveContext(contextCaptor.capture(), any(), any());
    return contextCaptor.getValue();
    }
}
