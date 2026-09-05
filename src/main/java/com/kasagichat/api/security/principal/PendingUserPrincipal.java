package com.kasagichat.api.security.principal;

import java.io.Serializable;
import java.security.Principal;

/**
 * OAuth認証済みで、本登録前のユーザーを表す認証主体。
 *
 * <p>ブラウザから仮登録IDを受け取らず、このPrincipalをサーバーセッションから
 * 復元することで、操作対象の仮登録情報を特定する。</p>
 *
 * @param pendingUserId {@code PendingUsers} テーブルの内部ID
 */
public record PendingUserPrincipal(Long pendingUserId) implements Principal, Serializable {

    /**
     * Spring Securityが認証主体を識別するための名前を返す。
     *
     * @return 仮登録ユーザーIDの文字列表現
     */
    @Override
    public String getName() {
        return pendingUserId.toString();
    }
}