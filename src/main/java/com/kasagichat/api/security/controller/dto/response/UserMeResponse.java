package com.kasagichat.api.security.controller.dto.response;

import java.util.UUID;

import com.kasagichat.api.security.model.Users;

/**
 * ユーザー情報を返却する。
 * @param publicId 公開ユーザーID
 * @param displayName 表示名
 * @param avatarUrl アバターURL
 */
public record UserMeResponse(
    UUID publicId,
    String displayName,
    String avatarUrl
) {

    public static UserMeResponse from(Users user) {
        return new UserMeResponse(
            user.getPublicId(),
            user.getDisplayName(),
            user.getAvatarUrl()
        );
    }
}
