package com.kasagichat.api.security.controller.dto.response;

/**
 * 仮登録ユーザーの登録情報
 * @param displayName 表示名
 * @param avatarUrl アバターURL
 */
public record PendingUserResponse(
    String displayName,
    String avatarUrl
) {
}
