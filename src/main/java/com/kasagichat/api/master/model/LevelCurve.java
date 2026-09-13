package com.kasagichat.api.master.model;

import com.kasagichat.api.common.model.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * レベルごとの到達に必要な累計EXPを管理するマスタEntity。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "level_curves")
public class LevelCurve extends BaseTimeEntity {

    /**
     * レベル。
     */
    @Id
    private Integer level;

    /**
     * このレベルに到達するために必要な累計EXP。
     */
    @Column(nullable = false)
    private Integer requiredExp;
}
