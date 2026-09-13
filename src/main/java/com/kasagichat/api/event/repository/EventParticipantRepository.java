package com.kasagichat.api.event.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kasagichat.api.event.model.EventParticipant;

/**
 * イベントの参加状況を永続化するRepository。
 */
public interface EventParticipantRepository extends JpaRepository<EventParticipant, Long> {

    /**
     * イベントにおけるユーザーの参加記録を取得する。退出済みの記録も含む。
     *
     * @param eventId イベントの内部ID
     * @param userId ユーザーの内部ID
     * @return 該当する参加記録。参加したことがない場合は空
     */
    Optional<EventParticipant> findByEventIdAndUserId(Long eventId, Long userId);

    /**
     * イベントに現在参加しているユーザーの記録を取得する。
     *
     * @param eventId イベントの内部ID
     * @return 参加中の記録の一覧
     */
    List<EventParticipant> findByEventIdAndLeftAtIsNull(Long eventId);
}
