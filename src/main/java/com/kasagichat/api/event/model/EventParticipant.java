package com.kasagichat.api.event.model;

import java.time.Instant;

import com.kasagichat.api.common.model.BaseTimeEntity;
import com.kasagichat.api.security.model.Users;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * イベントへの参加状況を保持するEntity。
 *
 * <p>退出後に再参加した場合は、同じ行の退出日時をnullに戻す。</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    name = "event_participants",
    uniqueConstraints = @UniqueConstraint(name = "uk_event_participants_event_user", columnNames = {"event_id", "user_id"}),
    indexes = @Index(name = "idx_event_participants_user_id", columnList = "user_id")
)
public class EventParticipant extends BaseTimeEntity {

    /**
     * 参加記録の内部ID。
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 参加したイベント。
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    /**
     * 参加したユーザー。
     */
    // Usersは@SoftDeleteのため、LAZYにするとHibernateの起動に失敗する。
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    /**
     * 参加した日時。
     */
    @Column(nullable = false)
    private Instant joinedAt;

    /**
     * 退出した日時。参加中の場合はnull。
     */
    @Column
    private Instant leftAt;
}
