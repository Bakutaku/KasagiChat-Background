package com.kasagichat.api.security.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.kasagichat.api.security.model.UserAuth;
import com.kasagichat.api.security.model.enums.AuthProvider;

/**
 * 外部認証情報のリポジトリ。
 */
public interface UserAuthRepository extends JpaRepository<UserAuth, Long> {

    /**
     * 認証プロバイダーとプロバイダー側のユーザー識別子から認証情報を取得する。
     *
     * <p>関連するユーザー情報も同時に取得する。</p>
     *
     * @param provider 認証プロバイダー
     * @param subject プロバイダー側のユーザー識別子
     * @return 該当する認証情報。存在しない場合は空
     */
    @EntityGraph(attributePaths = "user")
    Optional<UserAuth> findByProviderAndSubject(AuthProvider provider, String subject);
}
