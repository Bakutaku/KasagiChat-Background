package com.kasagichat.api.event.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kasagichat.api.event.model.Event;

/**
 * イベントを永続化するRepository。
 */
public interface EventRepository extends JpaRepository<Event, Long> {

    /**
     * 公開IDからイベントを取得する。参加者かどうかの確認は呼び出し側で行う。
     *
     * @param publicId イベントの公開ID
     * @return 該当するイベント。存在しない場合は空
     */
    Optional<Event> findByPublicId(UUID publicId);

    /**
     * 招待コードからイベントを取得する。
     *
     * @param inviteCode 招待コード
     * @return 該当するイベント。存在しない場合は空
     */
    Optional<Event> findByInviteCode(String inviteCode);
}
