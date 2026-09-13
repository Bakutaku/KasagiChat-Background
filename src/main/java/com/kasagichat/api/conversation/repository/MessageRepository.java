package com.kasagichat.api.conversation.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kasagichat.api.conversation.model.Message;

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
}
