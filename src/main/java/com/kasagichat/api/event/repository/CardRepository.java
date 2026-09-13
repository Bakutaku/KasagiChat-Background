package com.kasagichat.api.event.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kasagichat.api.event.model.Card;

/**
 * 出会いカードを永続化するRepository。
 */
public interface CardRepository extends JpaRepository<Card, Long> {

    /**
     * 指定したユーザー宛てのカードを公開IDから取得する。他人宛てのカードは取得できない。
     *
     * @param publicId カードの公開ID
     * @param recipientId 受取人の内部ID
     * @return 該当するカード。存在しない、または他人宛ての場合は空
     */
    Optional<Card> findByPublicIdAndRecipientId(UUID publicId, Long recipientId);

    /**
     * イベントでの自分宛てのカードを相性スコアの高い順に取得する。
     *
     * @param eventId イベントの内部ID
     * @param recipientId 受取人の内部ID
     * @return カードの一覧
     */
    List<Card> findByEventIdAndRecipientIdOrderByScoreDesc(Long eventId, Long recipientId);

    /**
     * 自分宛てのカードを新しい順にすべて取得する。
     *
     * @param recipientId 受取人の内部ID
     * @return カードの一覧
     */
    List<Card> findByRecipientIdOrderByCreatedAtDesc(Long recipientId);
}
