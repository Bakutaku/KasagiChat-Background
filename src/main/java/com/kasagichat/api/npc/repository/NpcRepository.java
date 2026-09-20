package com.kasagichat.api.npc.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kasagichat.api.npc.model.Npc;

/**
 * NPCを永続化するRepository。
 */
public interface NpcRepository extends JpaRepository<Npc, Long> {

    /**
     * ユーザーのNPCを取得する。
     *
     * @param userId ユーザーの内部ID
     * @return 該当するNPC。未作成の場合は空
     */
    Optional<Npc> findByUserId(Long userId);

    /**
     * ユーザーのNPCが作成済みか確認する。
     *
     * @param userId ユーザーの内部ID
     * @return 作成済みならtrue
     */
    boolean existsByUserId(Long userId);

    /**
     * ユーザーのNPCが誕生済みか確認する。
     *
     * @param userId ユーザーの内部ID
     * @return bornAtが設定済みならtrue
     */
    boolean existsByUserIdAndBornAtIsNotNull(Long userId);
}
