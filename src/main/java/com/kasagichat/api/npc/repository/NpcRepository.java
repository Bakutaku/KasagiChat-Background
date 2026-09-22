package com.kasagichat.api.npc.repository;

import java.util.Collection;
import java.util.List;
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
     * NPC誕生状態や家の配置を安全に更新するため、ユーザーのNPCを排他ロックして取得する。
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

    /**
     * 複数ユーザーの分身の名前をまとめて取得する。イベント一覧の作成者名の表示に使う。
     *
     * @param userIds ユーザーの内部IDの一覧
     * @return ユーザーの内部IDと分身の名前の組
     */
    @Query("select n.user.id, n.name from Npc n where n.user.id in :userIds")
    List<Object[]> findNamesByUserIds(@Param("userIds") Collection<Long> userIds);

    /**
     * 複数ユーザーの分身の名前と見た目プリセットをまとめて取得する。出会いカードの表示に使う。
     *
     * @param userIds ユーザーの内部IDの一覧
     * @return ユーザーの内部ID・分身の名前・見た目プリセットIDの組
     */
    @Query("select n.user.id, n.name, n.presetId from Npc n where n.user.id in :userIds")
    List<Object[]> findNameAndPresetsByUserIds(@Param("userIds") Collection<Long> userIds);

    /**
     * 複数ユーザーの分身をまとめて取得する。マッチングで全参加者の話題を引くために使う。
     *
     * @param userIds ユーザーの内部IDの一覧
     * @return 該当する分身の一覧
     */
    List<Npc> findByUserIdIn(Collection<Long> userIds);
}
