package com.kasagichat.api.event.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    /**
     * 招待コードが使用済みか確認する。
     *
     * @param inviteCode 招待コード
     * @return 使用済みならtrue
     */
    boolean existsByInviteCode(String inviteCode);

    /**
     * ユーザーが一覧で見られるイベントを取得する。作成したイベントと、参加中のイベントが対象。
     *
     * @param userId ユーザーの内部ID
     * @return 開始日時の新しい順のイベント一覧
     */
    @Query("""
        select e from Event e
        where e.creator.id = :userId
           or e.id in (select p.event.id from EventParticipant p
                       where p.user.id = :userId and p.leftAt is null)
        order by e.startsAt desc, e.id desc
        """)
    List<Event> findVisibleTo(@Param("userId") Long userId);
}
