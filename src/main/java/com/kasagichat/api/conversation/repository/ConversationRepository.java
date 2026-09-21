package com.kasagichat.api.conversation.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kasagichat.api.conversation.model.Conversation;
import com.kasagichat.api.conversation.model.enums.ConversationScene;
import com.kasagichat.api.conversation.model.enums.ConversationStatus;
import com.kasagichat.api.conversation.model.enums.ConversationType;

import jakarta.persistence.LockModeType;

/**
 * 会話を永続化するRepository。
 */
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    /**
     * 指定したユーザーの会話を公開IDから取得する。他人の会話は取得できない。
     *
     * @param publicId 会話の公開ID
     * @param userId 操作するユーザーの内部ID
     * @return 該当する会話。存在しない、または他人の会話の場合は空
     */
    Optional<Conversation> findByPublicIdAndUserId(UUID publicId, Long userId);

    /**
     * メッセージ送信・振り返りの二重実行を防ぐため、会話を排他ロックして取得する。
     *
     * @param publicId 会話の公開ID
     * @param userId 操作するユーザーの内部ID
     * @return 該当する会話。存在しない、または他人の会話の場合は空
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Conversation c where c.publicId = :publicId and c.user.id = :userId")
    Optional<Conversation> findByPublicIdAndUserIdForUpdate(
        @Param("publicId") UUID publicId,
        @Param("userId") Long userId
    );

    /**
     * 再開対象の会話を取得する。同じ種別・シーンで、指定した状態以外の最新の会話を返す。
     *
     * @param userId ユーザーの内部ID
     * @param type 会話の種別
     * @param scene 練習シーン。PRACTICE以外はnull
     * @param status 除外する状態。通常はREVIEWED
     * @return 再開対象の会話。存在しない場合は空
     */
    Optional<Conversation> findFirstByUserIdAndTypeAndSceneAndStatusNotOrderByCreatedAtDesc(
        Long userId,
        ConversationType type,
        ConversationScene scene,
        ConversationStatus status
    );

    /**
     * ユーザーの指定した状態の会話を新しい順に取得する。
     *
     * @param userId ユーザーの内部ID
     * @param status 会話の状態
     * @return 会話の一覧
     */
    List<Conversation> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, ConversationStatus status);

    /**
     * ユーザーが直前に冒頭マスタを使った会話を取得する。同じ冒頭の連続を避けるために使う。
     *
     * @param userId ユーザーの内部ID
     * @return 直前の会話。存在しない場合は空
     */
    Optional<Conversation> findFirstByUserIdAndOpeningIsNotNullOrderByCreatedAtDesc(Long userId);
}
