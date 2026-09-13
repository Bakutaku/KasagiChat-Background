package com.kasagichat.api.npc.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kasagichat.api.npc.model.Memory;

/**
 * 話題にぶら下がる思い出を永続化するRepository。
 */
public interface MemoryRepository extends JpaRepository<Memory, Long> {

    /**
     * 話題の思い出を、出来事の新しい順に取得する。
     *
     * @param topicId 話題の内部ID
     * @return 思い出の一覧
     */
    List<Memory> findByTopicIdOrderByOccurredAtDesc(Long topicId);

    /**
     * NPCの直近の思い出を最大10件取得する。自分のNPCとの会話のプロンプトに使う。
     *
     * @param npcId NPCの内部ID
     * @return 直近の思い出の一覧
     */
    List<Memory> findTop10ByTopicNpcIdOrderByCreatedAtDesc(Long npcId);

    /**
     * 指定した話題のうち、公開された話題の思い出だけを取得する。カード生成に使う。
     *
     * @param topicIds 話題の内部IDの一覧
     * @return 公開話題の思い出の一覧
     */
    List<Memory> findByTopicIdInAndTopicPublicTopicTrue(Collection<Long> topicIds);
}
