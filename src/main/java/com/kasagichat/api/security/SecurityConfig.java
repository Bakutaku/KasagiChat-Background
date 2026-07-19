package com.kasagichat.api.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, OAuthLoginSuccessHandler successHandler) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health").permitAll()
                        .anyRequest().authenticated())
                // CSRF対策: トークンをJSから読めるCookie(XSRF-TOKEN)で配り、
                // 変更系リクエストはX-XSRF-TOKENヘッダでの返送を必須にする(Double Submit Cookie)
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler()))
                .oauth2Login(oauth -> oauth
                        // 認可開始/コールバックURLを /api 配下に置き、Next.jsのrewritesを通す(同一オリジン維持)
                        .authorizationEndpoint(a -> a.baseUri("/api/oauth2/authorization"))
                        .redirectionEndpoint(r -> r.baseUri("/api/login/oauth2/code/*"))
                        .successHandler(successHandler)
                        .failureHandler(new SimpleUrlAuthenticationFailureHandler("/login?error=oauth")))
                // 未認証のAPIアクセスはログイン画面へのリダイレクトではなく401を返す(SPA向け)
                .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .logout(logout -> logout
                        .logoutUrl("/api/logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler()));
        return http.build();
    }
}
