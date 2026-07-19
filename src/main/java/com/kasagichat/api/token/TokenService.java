package com.kasagichat.api.token;

import java.time.Instant;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import com.kasagichat.api.user.AppUser;

@Service
public class TokenService {

    private final JwtEncoder jwtEncoder;
    private final JwtProperties properties;

    public TokenService(JwtEncoder jwtEncoder, JwtProperties properties) {
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
    }

    /**
     * アクセストークン(JWT)を発行する。
     * ペイロードは署名されているだけで**暗号化はされていない**(Base64URLで誰でも読める)ため、
     * 秘密情報は入れない。subにはユーザーIDを入れる。
     */
    public String issueAccessToken(AppUser user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("kasagichat")
                .subject(String.valueOf(user.getId()))
                .claim("name", user.getDisplayName())
                .issuedAt(now)
                .expiresAt(now.plus(properties.accessTokenTtl()))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
