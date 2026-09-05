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

@Configuration
@EnableConfigurationProperties(SecurityProperties.class)
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

	private final SecurityProperties securityProperties;

	public SecurityConfig(SecurityProperties securityProperties) {
		this.securityProperties = securityProperties;
	}

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    AuthenticationFailureHandler oauthAuthenticationFailureHandler() {
        return new SimpleUrlAuthenticationFailureHandler(
                securityProperties.frontendUrl() + "/auth/error"
        );
    }

    /**
     * ブラウザからのOAuth/OIDCログインとセッション認証を扱う。
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
                // OAuthログイン・コールバック
                .requestMatchers(
                    "/oauth2/**",
                    "/login/oauth2/**",
                    "/error",
                    "/api/auth/csrf",
                    "/actuator/health"
                ).permitAll()
                // 未ログインでも利用可能
                .requestMatchers(
                    HttpMethod.GET, "/api/terms/required"
                ).permitAll()
                // 仮登録ユーザー
                .requestMatchers(
                    "/api/registrations/**"
                ).hasAuthority(SecurityConst.ROLE_PENDING_REGISTRATION)
                // 登録済みユーザー
                .requestMatchers(
                    "/api/**"
                ).hasAuthority(SecurityConst.ROLE_USER)
                // そのほか
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
