package com.kasagichat.api.security.controller.dto.response;

/**
 * アカウント作成画面に表示する仮登録ユーザー情報。
 *
 * @param displayName 表示名
 * @param avatarUrl アバターURL
 */
public record PendingUserResponse(
    String displayName,
    String avatarUrl
) {
}
