package com.kasagichat.api.event.service;

import java.security.SecureRandom;

import org.springframework.stereotype.Component;

import com.kasagichat.api.event.exception.EventException;
import com.kasagichat.api.event.repository.EventRepository;

import lombok.RequiredArgsConstructor;

/** 掲示やQRから手入力されるため、誤読しやすい文字(0/O/1/I/L)を除いた英数字で発行する。 */
@Component
@RequiredArgsConstructor
public class InviteCodeGenerator {

    private static final char[] ALPHABET = "23456789ABCDEFGHJKMNPQRSTUVWXYZ".toCharArray();

    private static final int LENGTH = 8;

    private static final int MAX_ATTEMPTS = 5;

    private final SecureRandom random = new SecureRandom();

    private final EventRepository eventRepository;

    /**
     * 未使用の招待コードを発行する。
     *
     * @return 8文字の招待コード
     * @throws EventException 試行回数の上限まで未使用のコードを引けなかった場合
     */
    public String generate() {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            String code = randomCode();
            // 保存時の一意制約が最終的な防御。ここでの確認は衝突時のやり直しを減らすためのもの。
            if (!eventRepository.existsByInviteCode(code)) {
                return code;
            }
        }
        throw EventException.inviteCodeGenerationFailed();
    }

    private String randomCode() {
        StringBuilder code = new StringBuilder(LENGTH);
        for (int index = 0; index < LENGTH; index++) {
            code.append(ALPHABET[random.nextInt(ALPHABET.length)]);
        }
        return code.toString();
    }
}
