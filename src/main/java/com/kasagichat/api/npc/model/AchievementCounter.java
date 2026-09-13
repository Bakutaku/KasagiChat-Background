package com.kasagichat.api.npc.model;

import com.kasagichat.api.common.model.BaseTimeEntity;
import com.kasagichat.api.master.model.CounterDef;
import com.kasagichat.api.security.model.Users;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
 * 実績判定と統計表示に使う、ユーザーごとの集計カウンターを保持するEntity。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    name = "achievement_counters",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_achievement_counters_user_counter",
        columnNames = {"user_id", "counter_def_id"}
    )
)
public class AchievementCounter extends BaseTimeEntity {

    /**
     * 集計カウンターの内部ID。
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * カウンターの対象ユーザー。
     */
    // Usersは@SoftDeleteのため、LAZYにするとHibernateの起動に失敗する。
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    /**
     * カウンターの種別。
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "counter_def_id", nullable = false)
    private CounterDef counterDef;

    /**
     * カウンターの現在値。
     */
    @Builder.Default
    @Column(nullable = false)
    private Long value = 0L;
}
