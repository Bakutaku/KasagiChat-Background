package com.kasagichat.api.npc.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kasagichat.api.npc.model.UnlockedItem;

/**
 * ユーザーが解禁したアイテムを永続化するRepository。
 */
public interface UnlockedItemRepository extends JpaRepository<UnlockedItem, Long> {

    /**
     * ユーザーが解禁したアイテムを取得する。
     *
     * @param userId ユーザーの内部ID
     * @return 解禁済みアイテムの一覧
     */
    List<UnlockedItem> findByUserId(Long userId);
}
