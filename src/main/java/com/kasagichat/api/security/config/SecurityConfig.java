package com.kasagichat.api.security.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

import com.kasagichat.api.security.SecurityProperties;
import com.kasagichat.api.security.SecurityConst;
import com.kasagichat.api.security.handler.OAuthLoginSuccessHandler;

/**
 * OAuthログイン、セッション認証およびAPI認可を構成する。
 */
@Configuration
@EnableConfigurationProperties(SecurityProperties.class)
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

	/**
	 * セキュリティ関連のアプリケーション設定。
	 */
	private final SecurityProperties securityProperties;

	/**
	 * セキュリティ設定を生成する。
	 *
	 * @param securityProperties セキュリティ関連のアプリケーション設定
	 */
	public SecurityConfig(SecurityProperties securityProperties) {
		this.securityProperties = securityProperties;
	}

    /**
     * 認証情報をHTTPセッションへ保存するRepositoryを生成する。
     *
     * @return HTTPセッションを使用するSecurityContext Repository
     */
    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    /**
     * OAuth認証失敗時にフロントエンドのエラー画面へ遷移するハンドラーを生成する。
     *
     * @return OAuth認証失敗ハンドラー
     */
    @Bean
    AuthenticationFailureHandler oauthAuthenticationFailureHandler() {
        return new SimpleUrlAuthenticationFailureHandler(
                securityProperties.frontendUrl() + "/auth/error"
        );
    }

    /**
     * ブラウザからのOAuth/OIDCログイン、セッション認証およびAPI認可を構成する。
     *
     * @param http HTTPセキュリティ設定
     * @param successHandler OAuth認証成功後の処理を行うハンドラー
     * @param securityContextRepository 認証情報を保存するRepository
     * @param authenticationFailureHandler OAuth認証失敗時のハンドラー
     * @return 構成済みのセキュリティフィルターチェーン
     * @throws Exception セキュリティ設定の構築に失敗した場合
     */
    @Bean
    SecurityFilterChain oidcFilterChain(
            HttpSecurity http,
            OAuthLoginSuccessHandler successHandler,
            SecurityContextRepository securityContextRepository,
            AuthenticationFailureHandler authenticationFailureHandler
    ) throws Exception {
        
		http
            .securityContext(security -> security
                .securityContextRepository(securityContextRepository)
            )
            .authorizeHttpRequests(auth -> auth
                // OAuthログインとコールバック。
                .requestMatchers(
                    "/oauth2/**",
                    "/login/oauth2/**",
                    "/error",
                    "/api/auth/csrf",
                    "/actuator/health"
                ).permitAll()
                // 未ログインでも利用できるエンドポイント。
                .requestMatchers(
                    HttpMethod.GET, "/api/terms/required"
                ).permitAll()
                // 仮登録ユーザーが利用できるエンドポイント。
                .requestMatchers(
                    "/api/registrations/**"
                ).hasAuthority(SecurityConst.ROLE_PENDING_REGISTRATION)
                // 登録済みユーザーが利用できるエンドポイント。
                .requestMatchers(
                    "/api/**"
                ).hasAuthority(SecurityConst.ROLE_USER)
                // その他のリクエストは拒否する。
                .anyRequest().denyAll()
            ).csrf(csrf -> csrf.spa())
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(
                    new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)
                )
                .accessDeniedHandler((request,response,exception) -> response.sendError(HttpStatus.FORBIDDEN.value()))
            )
            .oauth2Login(oauth -> oauth
                .successHandler(successHandler)
                .failureHandler(authenticationFailureHandler)
            )
            .logout(logout -> logout
                .logoutUrl("/api/logout")
                .invalidateHttpSession(true)
                .deleteCookies("SESSION","JSESSIONID")
                .logoutSuccessHandler(
                    new HttpStatusReturningLogoutSuccessHandler()
                )
            );
        return http.build();
    }
}
