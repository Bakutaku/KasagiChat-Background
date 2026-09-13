package com.kasagichat.api.master.model;

import com.kasagichat.api.common.model.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 実績判定に使う集計カウンターの種別を管理するマスタEntity。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    name = "counter_defs",
    uniqueConstraints = @UniqueConstraint(name = "uk_counter_defs_code", columnNames = "code")
)
public class CounterDef extends BaseTimeEntity {

    /**
     * カウンター種別の内部ID。
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * カウンター種別を表すコード。例: PRACTICE_CAFE。
     */
    @Column(nullable = false, length = 50)
    private String code;

    /**
     * カウンターの名称。
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * カウンターの説明。
     */
    @Column(length = 500)
    private String description;
}
