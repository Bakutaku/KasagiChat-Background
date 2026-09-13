package com.kasagichat.api.event.model;

import java.time.Instant;
import java.util.UUID;

import com.kasagichat.api.common.model.BaseTimeEntity;
import com.kasagichat.api.event.model.enums.VenueTemplate;
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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 招待コードで参加するイベントを保持するEntity。
 *
 * <p>開催前・開催中・終了のフェーズは開始・終了日時と早期終了日時から算出する。
 * 常設デモイベントも通常のイベントとして扱う。</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    name = "events",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_events_public_id", columnNames = "public_id"),
        @UniqueConstraint(name = "uk_events_invite_code", columnNames = "invite_code")
    },
    indexes = @Index(name = "idx_events_creator_user_id", columnList = "creator_user_id")
)
public class Event extends BaseTimeEntity {

    /**
     * イベントの内部ID。
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * APIのパスでイベントを識別する公開ID。
     */
    @Column(nullable = false, updatable = false)
    private UUID publicId;

    /**
     * イベントの作成者。
     */
    // Usersは@SoftDeleteのため、LAZYにするとHibernateの起動に失敗する。
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "creator_user_id", nullable = false)
    private Users creator;

    /**
     * イベントのタイトル。
     */
    @Column(nullable = false, length = 100)
    private String title;

    /**
     * イベントの説明。
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * 開催の開始日時。
     */
    @Column(nullable = false)
    private Instant startsAt;

    /**
     * 開催の終了日時。
     */
    @Column(nullable = false)
    private Instant endsAt;

    /**
     * 会場のテンプレート。
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private VenueTemplate venueTemplate;

    /**
     * 参加に使う招待コード。6〜8文字の英数字。
     */
    @Column(nullable = false, length = 8)
    private String inviteCode;

    /**
     * 作成者が早期終了した日時。終了していない場合はnull。
     */
    @Column
    private Instant archivedAt;

    /**
     * イベントの公開IDを生成する。
     */
    @PrePersist
    void generatePublicId() {
        if (publicId == null) {
            publicId = UUID.randomUUID();
        }
    }
}
