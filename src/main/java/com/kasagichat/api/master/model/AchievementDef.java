package com.kasagichat.api.master.model;

import com.kasagichat.api.common.model.BaseTimeEntity;
import com.kasagichat.api.master.model.enums.RewardType;

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
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 実績の達成条件と報酬を管理するマスタEntity。
 *
 * <p>カウンターが閾値を跨いだときに達成とする。実績を追加する場合はこのマスタに1行足す。</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    name = "achievement_defs",
    uniqueConstraints = @UniqueConstraint(name = "uk_achievement_defs_code", columnNames = "code"),
    indexes = @Index(name = "idx_achievement_defs_counter_def_id", columnList = "counter_def_id")
)
public class AchievementDef extends BaseTimeEntity {

    /**
     * 実績定義の内部ID。
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 実績を表すコード。
     */
    @Column(nullable = false, length = 50)
    private String code;

    /**
     * 実績の名称。
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * 実績の説明。
     */
    @Column(length = 500)
    private String description;

    /**
     * 達成条件の対象となるカウンター種別。
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "counter_def_id", nullable = false)
    private CounterDef counterDef;

    /**
     * 達成とみなすカウンターの値。
     */
    @Column(nullable = false)
    private Long threshold;

    /**
     * 報酬の種類。
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RewardType rewardType;

    /**
     * 報酬として解禁するアイテム。報酬の種類がITEMの場合のみ設定する。
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reward_item_id")
    private Item rewardItem;

    /**
     * 報酬として加算するEXP。報酬の種類がEXPの場合のみ設定する。
     */
    @Column
    private Integer rewardExp;

    /**
     * 実績が有効かどうか。
     */
    @Builder.Default
    @Column(nullable = false)
    private Boolean enabled = true;
}
