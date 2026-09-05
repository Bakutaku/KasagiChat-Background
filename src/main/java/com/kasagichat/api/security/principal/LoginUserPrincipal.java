package com.kasagichat.api.security.principal;

import java.io.Serializable;
import java.security.Principal;

/**
 * 本登録済みユーザーの認証主体。
 *
 * @param userId {@code Users} テーブルの内部ユーザーID
 */
public record LoginUserPrincipal(Long userId) implements Principal, Serializable {

    /**
     * Spring Securityが認証主体を識別するための名前を返す。
     *
     * @return ユーザーIDの文字列表現
     */
    @Override
    public String getName() {
        return userId.toString();
    }
}