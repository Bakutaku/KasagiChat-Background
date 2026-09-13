package com.kasagichat.api.npc.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kasagichat.api.npc.model.Npc;

import jakarta.persistence.LockModeType;

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
     * ユーザーのNPCを排他ロックして取得する。
     *
     * <p>プロフィール更新とEXP加算が同時に行われても、互いの変更を上書きしないために使う。</p>
     *
     * @param userId ユーザーの内部ID
     * @return 該当するNPC。未作成の場合は空
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select n from Npc n where n.user.id = :userId")
    Optional<Npc> findByUserIdForUpdate(@Param("userId") Long userId);
}
