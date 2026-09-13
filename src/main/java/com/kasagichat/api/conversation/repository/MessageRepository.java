package com.kasagichat.api.conversation.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kasagichat.api.conversation.model.Message;
import com.kasagichat.api.conversation.model.enums.MessageRole;

/**
 * 会話ログのメッセージを永続化するRepository。
 */
public interface MessageRepository extends JpaRepository<Message, Long> {

    /**
     * 会話のメッセージを連番の順に取得する。
     *
     * @param conversationId 会話の内部ID
     * @return メッセージの一覧
     */
    List<Message> findByConversationIdOrderBySeqAsc(Long conversationId);

    /**
     * ユーザーのすべての会話における、指定した発言者のメッセージ数を数える。利用状況の概算に使う。
     *
     * @param userId ユーザーの内部ID
     * @param role 発言者
     * @return メッセージ数
     */
    long countByConversationUserIdAndRole(Long userId, MessageRole role);
}
