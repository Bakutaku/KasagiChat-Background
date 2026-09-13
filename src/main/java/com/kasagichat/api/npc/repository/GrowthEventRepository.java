package com.kasagichat.api.npc.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

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
}
