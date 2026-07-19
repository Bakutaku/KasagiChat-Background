package com.kasagichat.api.token;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final RefreshTokenRepository repository;
    private final JwtProperties properties;

    public RefreshTokenService(RefreshTokenRepository repository, JwtProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    /** 新しいリフレッシュトークンを発行する。戻り値の生トークンはこの瞬間しか手に入らない。 */
    @Transactional
    public String issue(Long userId) {
        byte[] bytes = new byte[48]; // 384bitの乱数 = 総当たり不可能なエントロピー
        RANDOM.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        repository.save(new RefreshToken(userId, hash(raw), Instant.now().plus(properties.refreshTokenTtl())));
        return raw;
    }

    /**
     * リフレッシュトークンを消費する(ローテーション)。
     * 有効なら該当行を削除してユーザーIDを返す = **1つのトークンは1回しか使えない**。
     * 盗まれたトークンが後から使われても、正規クライアントが先に使っていれば無効になっている。
     */
    @Transactional
    public Optional<Long> consume(String raw) {
        return repository.findByTokenHash(hash(raw)).flatMap(token -> {
            repository.delete(token);
            if (token.getExpiresAt().isBefore(Instant.now())) {
                return Optional.empty();
            }
            return Optional.of(token.getUserId());
        });
    }

    /** ログアウト等での明示的な失効 */
    @Transactional
    public void revoke(String raw) {
        repository.deleteByTokenHash(hash(raw));
    }

    private static String hash(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
