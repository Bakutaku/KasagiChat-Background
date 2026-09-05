package com.kasagichat.api.security.controller.dto.response;

import java.util.UUID;

/**
 * ユーザー登録完了後のレスポンス
 * @param publicId 公開ユーザーID
 * @param displayName 表示名
 * @param avatarUrl アバターURL
 */
public record UserRegistrationResponse(
    UUID publicId,
    String displayName,
    String avatarUrl
) {
}
