package com.kasagichat.api.security.oauth;

import com.kasagichat.api.security.model.enums.AuthProvider;

/**
 * OAuthプロバイダーごとに異なるユーザー情報を、アプリケーション内で扱う共通形式にした値オブジェクト。
 *
 * <p>この情報はOAuthプロバイダーが認証した結果から生成される。フロントエンドから受け取った値を
 * {@code subject} などへ設定してはならない。</p>
 *
 * @param provider 認証に使用したOAuthプロバイダー
 * @param subject プロバイダー内でユーザーを一意に識別する変更されないID
 * @param displayName アカウント作成画面へ表示する表示名の初期値
 * @param avatarUrl アカウント作成時に引き継ぐアバター画像URL
 */
public record OAuthIdentity(
        AuthProvider provider,
        String subject,
        String displayName,
        String avatarUrl
) {
}
