package com.kasagichat.api.npc.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kasagichat.api.npc.model.GrowthEvent;

/**
 * 成長演出・通知ログを永続化するRepository。
 */
public interface GrowthEventRepository extends JpaRepository<GrowthEvent, Long> {

    /**
     * ユーザーの未読の通知を古い順に取得する。
     *
     * @param userId ユーザーの内部ID
     * @return 未読の通知の一覧
     */
    List<GrowthEvent> findByUserIdAndReadAtIsNullOrderByCreatedAtAsc(Long userId);

    /**
     * ユーザーの通知を、既読・未読を問わず新しい順に最大50件取得する。
     *
     * @param userId ユーザーの内部ID
     * @return 通知の一覧
     */
    List<GrowthEvent> findTop50ByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * ユーザーの未読の通知をすべて既読にする。
     *
     * @param userId ユーザーの内部ID
     * @param readAt 既読にした日時
     * @return 既読にした件数
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
        update GrowthEvent g
        set g.readAt = :readAt, g.updatedAt = :readAt
        where g.user.id = :userId and g.readAt is null
        """)
    int markAllAsRead(@Param("userId") Long userId, @Param("readAt") Instant readAt);
}
