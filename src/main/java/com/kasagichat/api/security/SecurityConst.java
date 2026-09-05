package com.kasagichat.api.security;

/**
 * セキュリティ関連の定数を定義するクラス
 */
public final class SecurityConst {

    private SecurityConst() {
    }

    /**
     * 認証関連のAPIのベースパス
     */
    public static final String AUTH_BASE_PATH = "/api/auth";
    
    /**
     * JWT認証におけるリフレッシュトークンのエンドポイント
     */
    public static final String REFRESH_JWT_ENDPOINT = "/token/refresh";

    /**
     * JWT認証におけるリフレッシュトークンの破棄エンドポイント
     */
    public static final String REVOKE_JWT_ENDPOINT = "/token/revoke";

    /**
     * JWT認証におけるリフレッシュトークンのエンドポイント
     */
    public static final String REFRESH_JWT_ENDPOINT_PATH = AUTH_BASE_PATH + REFRESH_JWT_ENDPOINT;

    /**
     * JWT認証におけるリフレッシュトークンの破棄エンドポイント
     */
    public static final String REVOKE_JWT_ENDPOINT_PATH = AUTH_BASE_PATH + REVOKE_JWT_ENDPOINT;

    public static final String ROLE_PENDING_REGISTRATION = "ROLE_PENDING_REGISTRATION";

    public static final String ROLE_USER = "ROLE_USER";
}
