package com.kasagichat.api.security.controller.dto.response;

import java.util.UUID;

import com.kasagichat.api.security.model.Users;

/**
 * ユーザー情報を返却する。
 * @param publicId 公開ユーザーID
 * @param displayName 表示名
 * @param avatarUrl アバターURL
 * @param onboarding 初回オンボーディングの進行状態
 */
public record UserMeResponse(
    UUID publicId,
    String displayName,
    String avatarUrl,
    OnboardingStatusResponse onboarding
) {

    public static UserMeResponse from(Users user, OnboardingStatusResponse onboarding) {
        return new UserMeResponse(
            user.getPublicId(),
            user.getDisplayName(),
            user.getAvatarUrl(),
            onboarding
        );
    }
}
