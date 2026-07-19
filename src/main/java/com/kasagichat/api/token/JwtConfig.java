package com.kasagichat.api.token;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

/**
 * JWTの署名/検証設定。
 *
 * サーバが1台なので共通鍵(HS256)を採用。発行者と検証者が同じ鍵を持つ前提の方式。
 * 将来「発行はこのサーバ・検証は別サーバ」に分かれる場合は、秘密鍵で署名し
 * 公開鍵で検証する非対称方式(RS256/ES256)に切り替える。
 */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {

    @Bean
    SecretKey jwtSecretKey(JwtProperties properties) {
        byte[] bytes = properties.secret().getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            // HS256はHMAC-SHA256なので256bit(32バイト)以上の鍵が必須
            throw new IllegalStateException("app.jwt.secret は32バイト以上にすること");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSecretKey));
    }

    /** oauth2ResourceServer(jwt) がこのBeanを使ってBearerトークンを検証する(exp検証込み) */
    @Bean
    JwtDecoder jwtDecoder(SecretKey jwtSecretKey) {
        return NimbusJwtDecoder.withSecretKey(jwtSecretKey).macAlgorithm(MacAlgorithm.HS256).build();
    }
}
