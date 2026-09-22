package com.kasagichat.api.npc.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kasagichat.api.npc.model.Topic;

/**
 * NPCが覚えた話題を永続化するRepository。
 */
public interface TopicRepository extends JpaRepository<Topic, Long> {

    /** 所有NPCの品物を関連するマスタ・会話とまとめて取得する。 */
    @EntityGraph(attributePaths = {"category", "sourceConversation", "sourceConversation.user"})
    List<Topic> findByNpcIdOrderByLearnedAtDescIdDesc(Long npcId);

    /** NPCは認証済みユーザーから取得したものに限定する。 */
    Optional<Topic> findByIdAndNpcId(Long id, Long npcId);

    boolean existsByNpcIdAndHomeSlotIdAndIdNot(Long npcId, String homeSlotId, Long id);

    /**
     * NPCの話題を、覚えた日時の新しい順に取得する。
     *
     * @param npcId NPCの内部ID
     * @return 話題の一覧
     */
    List<Topic> findByNpcIdOrderByLearnedAtDesc(Long npcId);

    /**
     * 指定した会話の振り返りで追加された話題を取得する。
     *
     * @param conversationId 会話の内部ID
     * @return 追加された話題
     */
    List<Topic> findBySourceConversationIdOrderByIdAsc(Long conversationId);

    /**
     * 指定したユーザーのNPCが持つ話題を取得する。他人の話題は取得できない。
     *
     * @param id 話題の内部ID
     * @param userId 操作するユーザーの内部ID
     * @return 該当する話題。存在しない、または他人の話題の場合は空
     */
    Optional<Topic> findByIdAndNpcUserId(Long id, Long userId);

    /**
     * 複数のNPCの公開話題のうち、カテゴリが付いているものをまとめて取得する。
     *
     * <p>マッチングはカテゴリ一致で行うため、分類なしの話題は対象にしない。
     * 参加者ごとに引くとN+1になるので1クエリでまとめる。</p>
     *
     * @param npcIds NPCの内部IDの一覧
     * @return カテゴリ付きの公開話題の一覧
     */
    @Query("""
        select t from Topic t join fetch t.category
        where t.npc.id in :npcIds and t.publicTopic = true
        order by t.id asc
        """)
    List<Topic> findPublicCategorizedByNpcIds(@Param("npcIds") Collection<Long> npcIds);

    /**
     * NPCの公開話題を取得する。カード開封時のプロンプトに入れる範囲を公開話題だけに限るために使う。
     *
     * @param npcId NPCの内部ID
     * @return 公開話題の一覧
     */
    @EntityGraph(attributePaths = "category")
    List<Topic> findByNpcIdAndPublicTopicIsTrueOrderByInterestDescIdAsc(Long npcId);
}
