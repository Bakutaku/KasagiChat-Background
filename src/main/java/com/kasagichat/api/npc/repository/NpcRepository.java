package com.kasagichat.api.npc.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

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
     * NPC誕生状態を更新するため、ユーザーのNPCを排他ロックして取得する。
     *
     * @param userId ユーザーの内部ID
     * @return 該当するNPC。未作成の場合は空
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select n from Npc n where n.user.id = :userId")
    Optional<Npc> findByUserIdForUpdate(@Param("userId") Long userId);

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
