package com.kasagichat.api.security;

/**
 * セキュリティ関連の定数を定義するユーティリティクラス。
 */
public final class SecurityConst {

    /**
     * インスタンス化を禁止する。
     */
    private SecurityConst() {
    }

    /**
     * 本登録前のユーザーに付与する権限。
     */
    public static final String ROLE_PENDING_REGISTRATION = "ROLE_PENDING_REGISTRATION";

    /**
     * 本登録済みユーザーに付与する権限。
     */
    public static final String ROLE_USER = "ROLE_USER";
}
