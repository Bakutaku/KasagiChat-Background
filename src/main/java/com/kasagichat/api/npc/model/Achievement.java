package com.kasagichat.api.npc.model;

import java.time.Instant;

import com.kasagichat.api.common.model.BaseTimeEntity;
import com.kasagichat.api.master.model.AchievementDef;
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
 * ユーザーが達成した実績を保持するEntity。
 *
 * <p>報酬の受取日時がnullの行は、カササギが届ける未開封の箱として扱う。</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    name = "achievements",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_achievements_user_def",
        columnNames = {"user_id", "achievement_def_id"}
    )
)
public class Achievement extends BaseTimeEntity {

    /**
     * 実績達成記録の内部ID。
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 実績を達成したユーザー。
     */
    // Usersは@SoftDeleteのため、LAZYにするとHibernateの起動に失敗する。
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    /**
     * 達成した実績の定義。
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "achievement_def_id", nullable = false)
    private AchievementDef achievementDef;

    /**
     * 実績を達成した日時。
     */
    @Column(nullable = false)
    private Instant achievedAt;

    /**
     * 報酬を受け取った日時。未受取の場合はnull。
     */
    @Column
    private Instant claimedAt;
}
