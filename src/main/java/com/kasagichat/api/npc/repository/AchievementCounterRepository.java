package com.kasagichat.api.npc.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kasagichat.api.npc.model.AchievementCounter;

/**
 * ユーザーごとの集計カウンターを永続化するRepository。
 */
public interface AchievementCounterRepository extends JpaRepository<AchievementCounter, Long> {

    /**
     * ユーザーの指定した種別のカウンターを取得する。
     *
     * @param userId ユーザーの内部ID
     * @param counterCode カウンター種別のコード
     * @return 該当するカウンター。まだ集計していない場合は空
     */
    Optional<AchievementCounter> findByUserIdAndCounterDefCode(Long userId, String counterCode);
}
