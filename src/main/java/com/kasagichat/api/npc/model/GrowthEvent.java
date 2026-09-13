package com.kasagichat.api.npc.model;

import java.time.Instant;

import com.kasagichat.api.common.model.BaseTimeEntity;
import com.kasagichat.api.npc.model.enums.GrowthEventType;
import com.kasagichat.api.security.model.Users;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 成長演出や実績達成の通知ログを保持するEntity。未読分は家で読み返せる。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    name = "growth_events",
    indexes = @Index(name = "idx_growth_events_user_read_at", columnList = "user_id, read_at")
)
public class GrowthEvent extends BaseTimeEntity {

    /**
     * 通知の内部ID。
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 通知の対象ユーザー。
     */
    // Usersは@SoftDeleteのため、LAZYにするとHibernateの起動に失敗する。
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    /**
     * 通知の種類。
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private GrowthEventType type;

    /**
     * 画面に表示する演出テキスト。
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    /**
     * 既読にした日時。未読の場合はnull。
     */
    @Column
    private Instant readAt;
}
