package com.kasagichat.api.security.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kasagichat.api.security.model.PendingUsers;
import com.kasagichat.api.security.model.enums.AuthProvider;

/**
 * OAuth認証済み・本登録前のユーザー情報を永続化するRepository。
 */
public interface PendingUsersRepository extends JpaRepository<PendingUsers, Long> {

    /**
     * OAuthプロバイダー内のユーザー識別情報から仮登録情報を検索する。
     *
     * @param provider 認証に使用したOAuthプロバイダー
     * @param subject プロバイダー内で一意なユーザー識別子
     * @return 対応する仮登録情報。存在しない場合は空
     */
    Optional<PendingUsers> findByProviderAndSubject(
            AuthProvider provider,
            String subject
    );
}
