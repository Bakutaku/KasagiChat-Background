package com.kasagichat.api.credential.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

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
     * LLM呼び出し中の設定とデモ利用回数をユーザー単位で直列化する。
     *
     * @param userId ユーザーの内部ID
     * @return 該当する設定。未設定の場合は空
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ApiCredential c where c.user.id = :userId")
    Optional<ApiCredential> findByUserIdForUpdate(@Param("userId") Long userId);

    /**
     * ユーザーがAPIキーまたはデモ利用を設定済みか確認する。
     *
     * @param userId ユーザーの内部ID
     * @return 設定済みならtrue
     */
    boolean existsByUserId(Long userId);
}
