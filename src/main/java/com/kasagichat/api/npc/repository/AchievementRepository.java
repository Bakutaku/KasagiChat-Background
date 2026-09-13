package com.kasagichat.api.npc.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    /**
     * ユーザーの実績を、達成日時の新しい順にすべて取得する。一覧表示のため、実績定義と報酬アイテムも一緒に取得する。
     *
     * @param userId ユーザーの内部ID
     * @return 実績の一覧
     */
    @EntityGraph(attributePaths = {"achievementDef", "achievementDef.rewardItem"})
    List<Achievement> findByUserIdOrderByAchievedAtDesc(Long userId);

    /**
     * ユーザーの報酬未受取の実績を、達成日時の新しい順に取得する。
     *
     * @param userId ユーザーの内部ID
     * @return 未受取の実績の一覧
     */
    @EntityGraph(attributePaths = {"achievementDef", "achievementDef.rewardItem"})
    List<Achievement> findByUserIdAndClaimedAtIsNullOrderByAchievedAtDesc(Long userId);

    /**
     * ユーザーの報酬受取済みの実績を、達成日時の新しい順に取得する。
     *
     * @param userId ユーザーの内部ID
     * @return 受取済みの実績の一覧
     */
    @EntityGraph(attributePaths = {"achievementDef", "achievementDef.rewardItem"})
    List<Achievement> findByUserIdAndClaimedAtIsNotNullOrderByAchievedAtDesc(Long userId);

    /**
     * 指定したユーザーの実績を取得する。他人の実績は取得できない。
     *
     * @param id 実績達成記録の内部ID
     * @param userId 操作するユーザーの内部ID
     * @return 該当する実績。存在しない、または他人の実績の場合は空
     */
    @EntityGraph(attributePaths = {"achievementDef", "achievementDef.rewardItem"})
    Optional<Achievement> findByIdAndUserId(Long id, Long userId);

    /**
     * 報酬未受取の実績に受取日時を設定する。同時に受け取った場合でも、更新できるのは1回だけ。
     *
     * @param id 実績達成記録の内部ID
     * @param userId 操作するユーザーの内部ID
     * @param claimedAt 受取日時
     * @return 更新した件数。未受取の自分の実績であれば1、それ以外は0
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
        update Achievement a
        set a.claimedAt = :claimedAt, a.updatedAt = :claimedAt
        where a.id = :id and a.user.id = :userId and a.claimedAt is null
        """)
    int markClaimed(@Param("id") Long id, @Param("userId") Long userId, @Param("claimedAt") Instant claimedAt);
}
