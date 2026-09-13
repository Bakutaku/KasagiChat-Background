package com.kasagichat.api.npc.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kasagichat.api.npc.model.Achievement;

/**
 * 実績の達成記録を永続化するRepository。
 */
public interface AchievementRepository extends JpaRepository<Achievement, Long> {

    /**
     * ユーザーの報酬未受取の実績を取得する。
     *
     * @param userId ユーザーの内部ID
     * @return 未受取の実績の一覧
     */
    List<Achievement> findByUserIdAndClaimedAtIsNull(Long userId);
}
