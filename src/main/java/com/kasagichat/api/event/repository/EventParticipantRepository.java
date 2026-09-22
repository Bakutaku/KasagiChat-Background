package com.kasagichat.api.event.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kasagichat.api.event.controller.dto.response.EventParticipantResponse;
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

    /**
     * 会場の賑わい表示に使う参加者を取得する。
     *
     * <p>{@code EventParticipant.user} は{@code Users}が{@code @SoftDelete}のためEAGERで、
     * Entityを辿るとN+1になる。分身まで結合した射影を1クエリで返すことで、ユーザーを読み込まずに済ませる。
     * 並び順は座席の割り当て順そのものなので、参加日時で固定する。</p>
     *
     * @param eventId イベントの内部ID
     * @return 参加日時の古い順の参加者一覧
     */
    @Query("""
        select new com.kasagichat.api.event.controller.dto.response.EventParticipantResponse(n.name, n.presetId)
        from EventParticipant p
        join Npc n on n.user.id = p.user.id
        where p.event.id = :eventId and p.leftAt is null
        order by p.joinedAt asc, p.id asc
        """)
    List<EventParticipantResponse> findVenueParticipants(@Param("eventId") Long eventId);

    /**
     * イベントごとの参加中の人数を1クエリでまとめて数える。
     *
     * @param eventIds イベントの内部IDの一覧
     * @return イベントの内部IDと人数の組
     */
    @Query("""
        select p.event.id, count(p) from EventParticipant p
        where p.event.id in :eventIds and p.leftAt is null
        group by p.event.id
        """)
    List<Object[]> countActiveByEventIds(@Param("eventIds") Collection<Long> eventIds);

    /**
     * イベントに参加中のユーザーの人数を数える。
     *
     * @param eventId イベントの内部ID
     * @return 参加中の人数
     */
    long countByEventIdAndLeftAtIsNull(Long eventId);

    /**
     * 指定したユーザー以外の参加記録があるか確認する。削除の可否判定に使う。
     *
     * <p>退出済みの記録も対象にする。退出者宛てのカードがイベントを参照しているため、
     * 参加中の人数だけで判定すると削除時に外部キー違反になる。</p>
     *
     * @param eventId イベントの内部ID
     * @param userId 除外するユーザーの内部ID
     * @return 他のユーザーの参加記録があればtrue
     */
    boolean existsByEventIdAndUserIdNot(Long eventId, Long userId);

    /**
     * イベントの参加記録をすべて削除する。イベント本体より先に消す必要がある。
     *
     * @param eventId イベントの内部ID
     */
    @Modifying
    void deleteByEventId(Long eventId);

    /**
     * ユーザーが参加中のイベントの内部IDを取得する。一覧で参加状態をまとめて判定するために使う。
     *
     * @param userId ユーザーの内部ID
     * @return 参加中のイベントの内部IDの一覧
     */
    @Query("select p.event.id from EventParticipant p where p.user.id = :userId and p.leftAt is null")
    List<Long> findActiveEventIds(@Param("userId") Long userId);
}
