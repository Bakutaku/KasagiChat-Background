package com.kasagichat.api.credential.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kasagichat.api.credential.model.DemoUsage;

/**
 * デモ利用回数を永続化するRepository。
 */
public interface DemoUsageRepository extends JpaRepository<DemoUsage, Long> {

    /**
     * ユーザーのデモ利用回数を取得する。
     *
     * @param userId ユーザーの内部ID
     * @return 該当する利用回数。まだ利用していない場合は空
     */
    Optional<DemoUsage> findByUserId(Long userId);
}
