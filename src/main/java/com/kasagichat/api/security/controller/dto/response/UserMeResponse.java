package com.kasagichat.api.security.controller.dto.response;

import java.util.UUID;

import com.kasagichat.api.security.model.Users;

/**
 * ユーザー情報を返却する。
 * @param publicId 公開ユーザーID
 * @param displayName 表示名
 * @param avatarUrl アバターURL
 * @param onboarding 初回フローの進み具合
 */
public record UserMeResponse(
    UUID publicId,
    String displayName,
    String avatarUrl,
    OnboardingResponse onboarding
) {

    /**
     * ユーザーと初回フローの進み具合からレスポンスを生成する。
     *
     * @param user ユーザー
     * @param onboarding 初回フローの進み具合
     * @return ユーザー情報のレスポンス
     */
    public static UserMeResponse from(Users user, OnboardingResponse onboarding) {
        return new UserMeResponse(
            user.getPublicId(),
            user.getDisplayName(),
            user.getAvatarUrl(),
            onboarding
        );
    }
}
