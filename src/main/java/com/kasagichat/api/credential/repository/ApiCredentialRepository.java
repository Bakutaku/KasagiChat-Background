package com.kasagichat.api.credential.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kasagichat.api.credential.model.ApiCredential;

/**
 * ユーザーのAPIキー設定を永続化するRepository。
 */
public interface ApiCredentialRepository extends JpaRepository<ApiCredential, Long> {

    /**
     * ユーザーのAPIキー設定を取得する。
     *
     * @param userId ユーザーの内部ID
     * @return 該当する設定。未設定の場合は空
     */
    Optional<ApiCredential> findByUserId(Long userId);

    /**
     * ユーザーがAPIキーまたはデモの設定を済ませているかどうかを確認する。
     *
     * @param userId ユーザーの内部ID
     * @return 設定済みの場合はtrue
     */
    boolean existsByUserId(Long userId);
}
