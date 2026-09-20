package com.kasagichat.api.credential.crypto;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;

import com.kasagichat.api.credential.LlmProperties;
import com.kasagichat.api.credential.exception.LlmConfigurationException;

/**
 * APIキーをAES-256-GCMで暗号化・復号する。
 *
 * <p>保存形式は「バージョン1バイト + 12バイトIV + 認証タグ付き暗号文」。</p>
 */
@Component
public class ApiKeyCipher {

    private static final byte FORMAT_VERSION = 1;
    private static final int KEY_LENGTH_BYTES = 32;
    private static final int IV_LENGTH_BYTES = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;

    private final LlmProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

    public ApiKeyCipher(LlmProperties properties) {
        this.properties = properties;
    }

    /**
     * APIキーを暗号化する。
     *
     * @param apiKey 平文のAPIキー
     * @return バージョンとIVを含む暗号文
     */
    public byte[] encrypt(String apiKey) {
        byte[] iv = new byte[IV_LENGTH_BYTES];
        secureRandom.nextBytes(iv);
        try {
            Cipher cipher = newCipher(Cipher.ENCRYPT_MODE, iv);
            byte[] encrypted = cipher.doFinal(apiKey.getBytes(StandardCharsets.UTF_8));
            return ByteBuffer.allocate(1 + iv.length + encrypted.length)
                .put(FORMAT_VERSION)
                .put(iv)
                .put(encrypted)
                .array();
        } catch (GeneralSecurityException exception) {
            throw new LlmConfigurationException("APIキーを安全に保存できませんでした");
        }
    }

    /**
     * 保存済みのAPIキーを復号する。復号値はクライアントへ返してはならない。
     *
     * @param payload バージョンとIVを含む暗号文
     * @return 平文のAPIキー
     */
    public String decrypt(byte[] payload) {
        if (payload == null || payload.length <= 1 + IV_LENGTH_BYTES || payload[0] != FORMAT_VERSION) {
            throw new LlmConfigurationException("保存済みAPIキーの形式が不正です");
        }
        byte[] iv = new byte[IV_LENGTH_BYTES];
        byte[] encrypted = new byte[payload.length - 1 - IV_LENGTH_BYTES];
        System.arraycopy(payload, 1, iv, 0, iv.length);
        System.arraycopy(payload, 1 + iv.length, encrypted, 0, encrypted.length);
        try {
            Cipher cipher = newCipher(Cipher.DECRYPT_MODE, iv);
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException exception) {
            throw new LlmConfigurationException("保存済みAPIキーを復号できませんでした");
        }
    }

    private Cipher newCipher(int mode, byte[] iv) throws GeneralSecurityException {
        byte[] key;
        try {
            key = Base64.getDecoder().decode(properties.credentialEncryptionKey());
        } catch (IllegalArgumentException exception) {
            throw new LlmConfigurationException("APIキー暗号化鍵はBase64形式で設定してください");
        }
        if (key.length != KEY_LENGTH_BYTES) {
            throw new LlmConfigurationException("APIキー暗号化鍵は32バイトで設定してください");
        }
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(mode, new SecretKeySpec(key, "AES"), new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
        return cipher;
    }
}
