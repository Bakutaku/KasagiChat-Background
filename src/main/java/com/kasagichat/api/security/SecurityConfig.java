package com.kasagichat.api.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * 認証方式は2本立て(同じAPIをどちらでも呼べる):
 *
 * 1. Bearerトークンチェーン … Authorizationヘッダ付きリクエスト用(将来のUE版などネイティブクライアント)
 * 2. セッションチェーン     … ブラウザ用(OAuthログイン + セッションCookie)
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Bearerトークン用チェーン。
     * - Cookieを使わない認証なのでCSRF対策は不要(CSRFは「Cookieが勝手に送られる」ことを悪用する攻撃)
     * - セッションも作らない(STATELESS)
     * - /api/token/refresh, /api/token/revoke は「リフレッシュトークン自体が資格情報」なのでここに含めて認証不要にする
     */
    @Bean
    @Order(1)
    SecurityFilterChain bearerTokenChain(HttpSecurity http) throws Exception {
        RequestMatcher tokenApi = request -> {
            String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                return true;
            }
            String path = request.getRequestURI();
            return "/api/token/refresh".equals(path) || "/api/token/revoke".equals(path);
        };

        http
                .securityMatcher(tokenApi)
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/token/refresh", "/api/token/revoke").permitAll()
                        .anyRequest().authenticated())
                // JwtConfigのJwtDecoderで署名とexpを検証し、Jwtをprincipalにする
                .oauth2ResourceServer(rs -> rs.jwt(Customizer.withDefaults()))
                .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));
        return http.build();
    }

    /** ブラウザ用チェーン(OAuthログイン + セッションCookie)。上のチェーンに該当しないリクエストはこちら。 */
    @Bean
    @Order(2)
    SecurityFilterChain sessionChain(HttpSecurity http, OAuthLoginSuccessHandler successHandler,
            @Value("${app.public-url}") String publicUrl) throws Exception {
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
                        .failureHandler(new SimpleUrlAuthenticationFailureHandler(publicUrl + "/login?error=oauth")))
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
